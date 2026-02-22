package com.podpirate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.podpirate.ui.components.PodcastCard
import com.podpirate.ui.screens.viewmodels.SearchViewModel

@Composable
fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val subscribing by viewModel.subscribing.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.query.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search for podcasts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.itunesId }) { result ->
                    PodcastCard(
                        title = result.title,
                        author = result.author,
                        artworkUrl = result.artworkUrl,
                        actionLabel = if (subscribing.contains(result.itunesId)) "..." else "Subscribe",
                        onAction = { viewModel.subscribe(result) },
                    )
                }
            }
        }
    }
}
