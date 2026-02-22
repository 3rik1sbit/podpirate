package com.podpirate.service

import com.podpirate.config.PodPirateProperties
import com.podpirate.model.AdSegment
import com.podpirate.model.EpisodeStatus
import com.podpirate.repository.AdSegmentRepository
import com.podpirate.repository.EpisodeRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
class AudioProcessingService(
    private val episodeRepository: EpisodeRepository,
    private val adSegmentRepository: AdSegmentRepository,
    private val properties: PodPirateProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("processingExecutor")
    fun processAsync(episodeId: Long) {
        try {
            process(episodeId)
        } catch (e: Exception) {
            log.error("Failed to process audio for episode $episodeId", e)
            val episode = episodeRepository.findById(episodeId).orElseThrow()
            episodeRepository.save(episode.copy(status = EpisodeStatus.ERROR))
        }
    }

    @Transactional
    fun process(episodeId: Long) {
        val episode = episodeRepository.findById(episodeId).orElseThrow()
        val localPath = episode.localAudioPath ?: throw IllegalStateException("No local audio file")
        val adSegments = adSegmentRepository.findByEpisodeIdOrderByStartTime(episodeId)

        episodeRepository.save(episode.copy(status = EpisodeStatus.PROCESSING))

        val processedDir = Path.of(properties.processedDir)
        Files.createDirectories(processedDir)

        val extension = localPath.substringAfterLast(".")
        val outputPath = processedDir.resolve("episode_${episode.id}_clean.$extension")

        if (adSegments.isEmpty()) {
            // No ads detected - just copy the file
            Files.copy(Path.of(localPath), outputPath, StandardCopyOption.REPLACE_EXISTING)
        } else {
            // Use FFmpeg to cut out ad segments
            removeAdSegments(localPath, adSegments, outputPath.toString())
        }

        episodeRepository.save(episode.copy(
            processedAudioPath = outputPath.toString(),
            status = EpisodeStatus.READY,
        ))

        log.info("Processed episode ${episode.id}: ${episode.title}")
    }

    private fun removeAdSegments(inputPath: String, adSegments: List<AdSegment>, outputPath: String) {
        // Build FFmpeg filter to select non-ad segments
        val keepSegments = buildKeepSegments(adSegments)

        if (keepSegments.isEmpty()) {
            Files.copy(Path.of(inputPath), Path.of(outputPath))
            return
        }

        // Build complex filter for concatenating non-ad segments
        val filterParts = mutableListOf<String>()
        val streamLabels = mutableListOf<String>()

        keepSegments.forEachIndexed { i, (start, end) ->
            filterParts.add("[0:a]atrim=start=$start:end=$end,asetpts=PTS-STARTPTS[a$i]")
            streamLabels.add("[a$i]")
        }

        val concatFilter = "${streamLabels.joinToString("")}concat=n=${keepSegments.size}:v=0:a=1[out]"
        val fullFilter = filterParts.joinToString(";") + ";" + concatFilter

        val command = listOf(
            "ffmpeg", "-y", "-threads", "2", "-i", inputPath,
            "-filter_complex", fullFilter,
            "-map", "[out]",
            "-threads", "2",
            outputPath
        )

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.inputStream.bufferedReader().readText()
            throw RuntimeException("FFmpeg failed with exit code $exitCode: $error")
        }
    }

    private fun buildKeepSegments(adSegments: List<AdSegment>): List<Pair<Double, Double>> {
        val sorted = adSegments.sortedBy { it.startTime }
        val keep = mutableListOf<Pair<Double, Double>>()
        var current = 0.0

        for (ad in sorted) {
            if (ad.startTime > current) {
                keep.add(Pair(current, ad.startTime))
            }
            current = maxOf(current, ad.endTime)
        }

        // Add segment after last ad (FFmpeg will handle end-of-file)
        keep.add(Pair(current, 999999.0))

        return keep
    }
}
