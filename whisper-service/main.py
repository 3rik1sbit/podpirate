import json
import os
import tempfile
from fastapi import FastAPI, UploadFile, File
from fastapi.responses import StreamingResponse
from faster_whisper import WhisperModel

app = FastAPI(title="PodPirate Whisper Service")

model_size = os.getenv("WHISPER_MODEL", "base")
device = os.getenv("WHISPER_DEVICE", "cpu")
compute_type = os.getenv("WHISPER_COMPUTE_TYPE", "int8")

model = None


@app.on_event("startup")
def load_model():
    global model
    model = WhisperModel(model_size, device=device, compute_type=compute_type)


@app.get("/health")
def health():
    return {"status": "ok", "model": model_size}


@app.post("/transcribe")
async def transcribe(file: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".audio") as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    try:
        segments_iter, info = model.transcribe(tmp_path, beam_size=5)

        segments = []
        for segment in segments_iter:
            segments.append({
                "start": round(segment.start, 2),
                "end": round(segment.end, 2),
                "text": segment.text.strip(),
            })

        return {
            "language": info.language,
            "language_probability": round(info.language_probability, 2),
            "duration": round(info.duration, 2),
            "segments": segments,
        }
    finally:
        os.unlink(tmp_path)


@app.post("/transcribe-stream")
async def transcribe_stream(file: UploadFile = File(...)):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".audio") as tmp:
        content = await file.read()
        tmp.write(content)
        tmp_path = tmp.name

    def generate():
        try:
            segments_iter, info = model.transcribe(tmp_path, beam_size=5)
            for segment in segments_iter:
                yield json.dumps({
                    "start": round(segment.start, 2),
                    "end": round(segment.end, 2),
                    "text": segment.text.strip(),
                }) + "\n"
            yield json.dumps({
                "done": True,
                "language": info.language,
                "language_probability": round(info.language_probability, 2),
                "duration": round(info.duration, 2),
            }) + "\n"
        finally:
            os.unlink(tmp_path)

    return StreamingResponse(generate(), media_type="application/x-ndjson")
