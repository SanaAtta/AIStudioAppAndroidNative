package com.aiartgenerator.imagegenerator.videogenerator.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aiartgenerator.imagegenerator.videogenerator.model.ProjectCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.ProjectItem
import com.aiartgenerator.imagegenerator.videogenerator.model.ProjectType

class ProjectsController {
    var selectedType by mutableStateOf(ProjectType.AiImage)
        private set

    val filterTypes: List<ProjectType> get() = ProjectCatalog.filterTypes

    val filteredProjects: List<ProjectItem>
        get() = ProjectCatalog.projects.filter { it.type == selectedType }

    fun onTypeSelected(type: ProjectType) {
        selectedType = type
    }

    fun onProjectClick(projectId: String) {
        // Future: open project detail / preview
    }
}
