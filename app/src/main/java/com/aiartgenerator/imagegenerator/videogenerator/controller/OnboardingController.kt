package com.aiartgenerator.imagegenerator.videogenerator.controller

import com.aiartgenerator.imagegenerator.videogenerator.model.OnboardingCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.OnboardingPage

class OnboardingController {
    val pages: List<OnboardingPage> = OnboardingCatalog.pages

    fun isLastPage(pageIndex: Int): Boolean = pageIndex >= pages.lastIndex
}
