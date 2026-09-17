package com.aiartgenerator.imagegenerator.videogenerator.view.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.aiartgenerator.imagegenerator.videogenerator.R
import com.aiartgenerator.imagegenerator.videogenerator.billing.PremiumAccess
import com.aiartgenerator.imagegenerator.videogenerator.model.PremiumCatalog
import com.aiartgenerator.imagegenerator.videogenerator.model.PremiumFeature
import com.aiartgenerator.imagegenerator.videogenerator.model.SubscriptionPlan
import com.aiartgenerator.imagegenerator.videogenerator.view.common.AppLayoutMetrics
import com.aiartgenerator.imagegenerator.videogenerator.view.common.ResponsiveScreenRoot
import com.aiartgenerator.imagegenerator.videogenerator.view.common.rememberAppLayoutMetrics
import com.aiartgenerator.imagegenerator.videogenerator.view.common.responsiveContentWidth
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AllScreenBgGradientBrush
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.AppBarDivider
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeMuted
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeOnBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.HomeSeeAll
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.MyApplicationTheme
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumBadgeBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumFeatureBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumFeatureContainer
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumHeadlineGold
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanSelectedBorder
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanSelectedContent
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanSelectedEnd
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanSelectedStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumPlanUnselected
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumSaveBadge
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.PremiumSubscribeText
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.ProGradientStart
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.subscribeButtonShadow
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.subscribeGradientBackground
import com.aiartgenerator.imagegenerator.videogenerator.view.theme.white
import kotlinx.coroutines.delay

private val CarouselCorner = 24.dp
private val FeatureCorner = 20.dp
private val PlanCorner = 18.dp
private val SubscribeCorner = 16.dp
private const val AutoScrollDelayMs = 2_000L
private const val AutoScrollAnimMs = 350

private data class PremiumLayoutSpec(
    val contentPadding: Dp = 14.dp,
    val carouselPadding: Dp = 20.dp,
    val carouselHeightRatio: Float = 0.55f,
    val carouselHeightMin: Dp = 180.dp,
    val carouselHeightMax: Dp = 260.dp,
    val featureIconSize: Dp = 40.dp,
    val featureTitleSize: TextUnit = 10.sp,
    val featureSubtitleSize: TextUnit = 9.sp,
    val featureDividerHeight: Dp = 72.dp,
    val featureBarPaddingVertical: Dp = 14.dp,
    val headlineSize: TextUnit = 18.sp,
    val showHeadlineLaurels: Boolean = true,
    val planTitleSize: TextUnit = 15.sp,
    val planSubtitleSize: TextUnit = 11.sp,
    val planPriceSize: TextUnit = 17.sp,
    val planPricePeriodSize: TextUnit = 11.sp,
    val planContentPadding: Dp = 12.dp,
    val planCardHeight: Dp = 78.dp,
    val radioSize: Dp = 24.dp,
    val subscribeHeight: Dp = 56.dp,
    val subscribeTextSize: TextUnit = 18.sp,
    val sectionSpacingLarge: Dp = 22.dp,
    val sectionSpacingMedium: Dp = 18.dp,
    val sectionSpacingSmall: Dp = 12.dp,
    val contentMaxWidth: Dp? = null,
)

@Composable
private fun rememberPremiumLayoutSpec(appMetrics: AppLayoutMetrics): PremiumLayoutSpec {
    val screenWidthDp = appMetrics.screenWidthDp
    val screenHeightDp = appMetrics.screenHeightDp
    return remember(screenWidthDp, screenHeightDp) {
        val shortHeight = screenHeightDp < 640
        val veryShortHeight = screenHeightDp < 580

        val base = when {
            screenWidthDp < 340 -> PremiumLayoutSpec(
                contentPadding = 12.dp,
                carouselPadding = 14.dp,
                carouselHeightRatio = 0.48f,
                carouselHeightMin = 155.dp,
                carouselHeightMax = 210.dp,
                featureIconSize = 30.dp,
                featureTitleSize = 8.sp,
                featureSubtitleSize = 7.sp,
                featureDividerHeight = 58.dp,
                featureBarPaddingVertical = 10.dp,
                headlineSize = 15.sp,
                showHeadlineLaurels = false,
                planTitleSize = 14.sp,
                planSubtitleSize = 11.sp,
                planPriceSize = 16.sp,
                planPricePeriodSize = 10.sp,
                planContentPadding = 12.dp,
                radioSize = 24.dp,
                subscribeHeight = 52.dp,
                subscribeTextSize = 16.sp,
                sectionSpacingLarge = 16.dp,
                sectionSpacingMedium = 14.dp,
                sectionSpacingSmall = 10.dp,
            )
            screenWidthDp < 380 -> PremiumLayoutSpec(
                contentPadding = 12.dp,
                carouselPadding = 18.dp,
                carouselHeightRatio = 0.52f,
                carouselHeightMin = 170.dp,
                carouselHeightMax = 240.dp,
                featureIconSize = 34.dp,
                featureTitleSize = 9.sp,
                featureSubtitleSize = 8.sp,
                featureDividerHeight = 64.dp,
                headlineSize = 16.sp,
                planTitleSize = 15.sp,
                planSubtitleSize = 12.sp,
                planPriceSize = 18.sp,
                planContentPadding = 14.dp,
                radioSize = 26.dp,
                sectionSpacingLarge = 18.dp,
                sectionSpacingMedium = 16.dp,
            )
            screenWidthDp > 420 -> PremiumLayoutSpec(
                carouselHeightRatio = 0.58f,
                carouselHeightMax = 280.dp,
                contentMaxWidth = 520.dp,
            )
            else -> PremiumLayoutSpec()
        }

        val heightAdjusted = when {
            veryShortHeight -> base.copy(
                carouselHeightRatio = base.carouselHeightRatio * 0.82f,
                carouselHeightMin = (base.carouselHeightMin.value * 0.82f).dp,
                carouselHeightMax = (base.carouselHeightMax.value * 0.72f).dp.coerceAtLeast(140.dp),
                featureDividerHeight = (base.featureDividerHeight.value * 0.78f).dp,
                featureBarPaddingVertical = 8.dp,
                planCardHeight = 68.dp,
                subscribeHeight = 50.dp,
                sectionSpacingLarge = (base.sectionSpacingLarge.value * 0.65f).dp,
                sectionSpacingMedium = (base.sectionSpacingMedium.value * 0.7f).dp,
                sectionSpacingSmall = 8.dp,
            )
            shortHeight -> base.copy(
                carouselHeightRatio = base.carouselHeightRatio * 0.9f,
                carouselHeightMax = (base.carouselHeightMax.value * 0.86f).dp,
                featureDividerHeight = (base.featureDividerHeight.value * 0.88f).dp,
                planCardHeight = 70.dp,
                sectionSpacingLarge = (base.sectionSpacingLarge.value * 0.78f).dp,
                sectionSpacingMedium = (base.sectionSpacingMedium.value * 0.82f).dp,
            )
            else -> base
        }

        heightAdjusted.copy(
            contentMaxWidth = heightAdjusted.contentMaxWidth ?: appMetrics.contentMaxWidth,
            contentPadding = appMetrics.horizontalPadding,
        )
    }
}

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onSubscribe: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    val appMetrics = rememberAppLayoutMetrics()
    val controller = rememberPremiumController()
    val layout = rememberPremiumLayoutSpec(appMetrics)
    val slides = PremiumCatalog.carouselSlides
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val isPremium by PremiumAccess.isPremiumUserFlow.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(isPremium) {
        if (isPremium) onSubscribe()
    }

    LaunchedEffect(slides.size) {
        if (slides.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AutoScrollDelayMs)
            val nextPage = (pagerState.currentPage + 1) % slides.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(
                    durationMillis = AutoScrollAnimMs,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    ResponsiveScreenRoot(
        modifier = modifier,
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                PremiumCarousel(
                    slides = slides,
                    pagerState = pagerState,
                    onClose = onBack,
                    layout = layout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(
                            appMetrics.copy(contentMaxWidth = layout.contentMaxWidth),
                        )
                        .padding(top = 18.dp)
                        .padding(horizontal = layout.carouselPadding),
                )

                Spacer(modifier = Modifier.height(layout.sectionSpacingSmall))

                PremiumFeatureBar(
                    features = PremiumCatalog.features,
                    layout = layout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(
                            appMetrics.copy(contentMaxWidth = layout.contentMaxWidth),
                        )
                        .padding(horizontal = layout.contentPadding),
                )

                Spacer(modifier = Modifier.height(layout.sectionSpacingLarge))

                PremiumHeadline(
                    layout = layout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(
                            appMetrics.copy(contentMaxWidth = layout.contentMaxWidth),
                        )
                        .padding(horizontal = layout.contentPadding),
                )

                Spacer(modifier = Modifier.height(layout.sectionSpacingMedium))

                controller.displayPlans.forEach { plan ->
                    PremiumPlanCard(
                        plan = plan,
                        selected = controller.selectedPlan == plan.id,
                        onClick = { controller.onPlanSelected(plan.id) },
                        layout = layout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .responsiveContentWidth(
                                appMetrics.copy(contentMaxWidth = layout.contentMaxWidth),
                            )
                            .padding(horizontal = layout.contentPadding, vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AllScreenBgGradientBrush),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(
                            appMetrics.copy(contentMaxWidth = layout.contentMaxWidth),
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = layout.contentPadding),
                ) {
                    // PremiumTrialRow(
                    //     layout = layout,
                    //     modifier = Modifier
                    //         .fillMaxWidth()
                    //         .padding(vertical = 8.dp),
                    // )

                    PremiumSubscribeButton(
                        onClick = { controller.subscribe(onSubscribe) },
                        enabled = controller.canSubscribe,
                        isLoading = controller.isPurchasing,
                        layout = layout,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    PremiumFooter(
                        layout = layout,
                        onRestoreClick = {
                            controller.restorePurchases { active ->
                                if (active) {
                                    onSubscribe()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.premium_restore_none),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        restoreEnabled = controller.canSubscribe || controller.isBillingReady,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumCarousel(
    slides: List<com.aiartgenerator.imagegenerator.videogenerator.model.PremiumCarouselSlide>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onClose: () -> Unit,
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val carouselHeight = (maxWidth * layout.carouselHeightRatio)
            .coerceIn(layout.carouselHeightMin, layout.carouselHeightMax)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(
                        PagerDefaults.pageNestedScrollConnection(
                            pagerState,
                            Orientation.Horizontal,
                        ),
                    ),
                pageSpacing = 10.dp,
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
            ) { pageIndex ->
                val slide = slides[pageIndex]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(CarouselCorner)),
                ) {
                    Image(
                        painter = painterResource(slide.imageRes),
                        contentDescription = stringResource(slide.labelRes),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(slide.labelRes),
                            color = white,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.premium_close),
                    tint = white,
                    modifier = Modifier.size(16.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(slides.size) { index ->
                        val selected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .then(
                                    if (selected) {
                                        Modifier.width(20.dp)
                                    } else {
                                        Modifier.size(6.dp)
                                    },
                                )
                                .clip(
                                    if (selected) {
                                        RoundedCornerShape(3.dp)
                                    } else {
                                        CircleShape
                                    },
                                )
                                .background(
                                    if (selected) {
                                        ProGradientStart
                                    } else {
                                        white.copy(alpha = 0.35f)
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureBar(
    features: List<PremiumFeature>,
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(FeatureCorner))
            .background(PremiumFeatureContainer)
            .border(1.dp, PremiumFeatureBorder.copy(alpha = 0.55f), RoundedCornerShape(FeatureCorner))
            .padding(vertical = layout.featureBarPaddingVertical, horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            features.forEachIndexed { index, feature ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(layout.featureDividerHeight)
                            .background(AppBarDivider),
                    )
                }
                PremiumFeatureItem(
                    feature = feature,
                    layout = layout,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    feature: PremiumFeature,
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(feature.iconRes),
            contentDescription = null,
            modifier = Modifier.size(layout.featureIconSize),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(feature.titleRes),
            color = HomeOnBackground,
            fontSize = layout.featureTitleSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(feature.subtitleRes),
            color = HomeMuted,
            fontSize = layout.featureSubtitleSize,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = layout.featureSubtitleSize * 1.2f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PremiumHeadline(
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    if (layout.showHeadlineLaurels) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_right),
                contentDescription = null,
                tint = ProGradientStart,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            PremiumHeadlineText(layout = layout, modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_left),
                contentDescription = null,
                tint = ProGradientStart,
                modifier = Modifier.size(22.dp),
            )
        }
    } else {
        PremiumHeadlineText(
            layout = layout,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PremiumHeadlineText(
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.premium_unlock))
            append(" ")
            withStyle(SpanStyle(color = PremiumHeadlineGold, fontWeight = FontWeight.Bold)) {
                append(stringResource(R.string.premium_premium_word))
            }
            append(" ")
            append(stringResource(R.string.premium_features))
        },
        color = HomeOnBackground,
        fontSize = layout.headlineSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun PremiumPlanCard(
    plan: SubscriptionPlan,
    selected: Boolean,
    onClick: () -> Unit,
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(PlanCorner)
    val cardBorderColor = if (selected) PremiumPlanSelectedBorder else PremiumPlanBorder
    val selectedContent = PremiumPlanSelectedContent
    val selectedMuted = selectedContent.copy(alpha = 0.78f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.planCardHeight)
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(PremiumPlanSelectedEnd, PremiumPlanSelectedStart),
                            ),
                        )
                    } else {
                        Modifier.background(PremiumPlanUnselected)
                    },
                )
                .border(1.5.dp, cardBorderColor, shape)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = layout.planContentPadding,
                    vertical = layout.planContentPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumPlanRadio(selected = selected, size = layout.radioSize)

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(plan.titleRes),
                        color = if (selected) selectedContent else HomeMuted,
                        fontSize = layout.planTitleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
//                    plan.saveBadgeRes?.let { badgeRes ->
//                        Spacer(modifier = Modifier.width(6.dp))
//                        Text(
//                            text = stringResource(badgeRes),
//                            color = white,
//                            fontSize = 9.sp,
//                            fontWeight = FontWeight.SemiBold,
//                            maxLines = 1,
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(10.dp))
//                                .background(PremiumSaveBadge)
//                                .padding(horizontal = 6.dp, vertical = 2.dp),
//                        )
//                    }
                }
                plan.subtitleRes?.let { subtitleRes ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(subtitleRes),
                        color = if (selected) selectedMuted else HomeMuted,
                        fontSize = layout.planSubtitleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(min = 72.dp),
            ) {
                Text(
                    text = plan.priceText,
                    color = if (selected) selectedContent else HomeMuted,
                    fontSize = layout.planPriceSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(plan.pricePeriodRes),
                    color = if (selected) selectedMuted else HomeMuted,
                    fontSize = layout.planPricePeriodSize,
                    maxLines = 1,
                )
            }
        }

        if (plan.isBestChoice) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(12.dp))
                            .background(PremiumBadgeBackground)
                    .border(1.dp, PremiumPlanSelectedBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_premium_star),
                    contentDescription = null,
                    tint = PremiumHeadlineGold,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.premium_best_choice),
                    color = HomeOnBackground,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PremiumPlanRadio(
    selected: Boolean,
    size: Dp,
) {
    val innerSize = size - 6.dp
    if (selected) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, Color(0xFF9EB0FF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .clip(CircleShape)
                    .background(ProGradientStart),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = white,
                    modifier = Modifier.size((innerSize * 0.65f).coerceAtLeast(12.dp)),
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, Color(0xFF6B7194), CircleShape),
        )
    }
}

@Composable
private fun PremiumTrialRow(
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_premium_check),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.premium_trial_prefix))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.premium_trial_highlight))
                }
            },
            color = HomeOnBackground,
            fontSize = layout.planSubtitleSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun PremiumSubscribeButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    layout: PremiumLayoutSpec,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SubscribeCorner)
    val pulseTransition = rememberInfiniteTransition(label = "subscribePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled && !isLoading) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "subscribeScale",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(layout.subscribeHeight)
            .subscribeButtonShadow(shape = shape)
            .clip(shape)
            .subscribeGradientBackground(shape)
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(pulseScale)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    if (isLoading) R.string.premium_subscribing else R.string.premium_subscribe,
                ),
                color = PremiumSubscribeText.copy(alpha = if (enabled || isLoading) 1f else 0.55f),
                fontSize = layout.subscribeTextSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            if (!isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_premium_arrow),
                    contentDescription = null,
                    tint = white.copy(alpha = if (enabled) 1f else 0.55f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumFooter(
    layout: PremiumLayoutSpec,
    onRestoreClick: () -> Unit,
    restoreEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val footerSize = if (layout.showHeadlineLaurels) 11.sp else 10.sp
    Column(
        modifier = modifier.padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.premium_disclaimer),
            color = HomeMuted,
            fontSize = footerSize,
            textAlign = TextAlign.Center,
            lineHeight = footerSize * 1.45f,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.premium_footer_links),
            color = HomeSeeAll,
            fontSize = footerSize,
            textAlign = TextAlign.Center,
            lineHeight = footerSize * 1.45f,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.premium_restore),
            color = HomeSeeAll,
            fontSize = footerSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(enabled = restoreEnabled, onClick = onRestoreClick),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228, widthDp = 360, heightDp = 640)
@Composable
private fun PremiumScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        PremiumScreen(onBack = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1228, widthDp = 320, heightDp = 568, name = "Small phone")
@Composable
private fun PremiumScreenSmallPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        PremiumScreen(onBack = {})
    }
}
