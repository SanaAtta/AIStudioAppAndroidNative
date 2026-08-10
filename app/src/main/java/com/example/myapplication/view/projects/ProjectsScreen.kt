package com.example.myapplication.view.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.controller.ProjectsController
import com.example.myapplication.model.ProjectItem
import com.example.myapplication.model.ProjectType
import com.example.myapplication.view.theme.HomeBackground
import com.example.myapplication.view.theme.HomeNavSelected
import com.example.myapplication.view.theme.HomeOnBackground
import com.example.myapplication.view.theme.MyApplicationTheme

private val ChipIdle = Color(0xFF1C1C1E)
private val ChipIdleText = Color(0xFFB0B0B0)

@Composable
fun ProjectsScreen(modifier: Modifier = Modifier) {
    val controller = remember { ProjectsController() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Text(
            text = "Projects",
            color = HomeOnBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        FilterChipsRow(
            types = controller.filterTypes,
            selectedType = controller.selectedType,
            onTypeSelected = controller::onTypeSelected,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        if (controller.filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No projects yet",
                    color = ChipIdleText,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(controller.filteredProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { controller.onProjectClick(project.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    types: List<ProjectType>,
    selectedType: ProjectType,
    onTypeSelected: (ProjectType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        types.forEach { type ->
            val selected = type == selectedType
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (selected) HomeNavSelected else ChipIdle)
                    .clickable { onTypeSelected(type) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = type.label,
                    color = if (selected) Color.White else ChipIdleText,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        project.color.copy(alpha = 0.95f),
                        project.color.copy(alpha = 0.55f),
                    ),
                ),
            )
            .clickable(onClick = onClick),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ProjectsScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        ProjectsScreen()
    }
}
