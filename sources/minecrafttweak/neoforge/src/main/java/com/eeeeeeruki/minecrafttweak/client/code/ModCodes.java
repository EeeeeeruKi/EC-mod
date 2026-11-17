package com.eeeeeeruki.minecrafttweak.client.code;

/**
 * 他MODの設定の項目番号。
 * 番号は一度決めたら変えない。新しい項目は必ず一番下に足す。
 * 同じ番号に名前の候補をいくつも書くと、MODやバージョンの違いを吸収できる。
 */
public final class ModCodes {
    private ModCodes() {
    }

    /** Sodium / Embeddium / Rubidium（頭文字 s）。 */
    public static ReflectCodes sodium() {
        return new ReflectCodes("s", "minecrafttweak.code.provider.s", true, new String[][]{
                {"net.caffeinemc.mods.sodium.client.SodiumClientMod", "options"},
                {"org.embeddedt.embeddium.impl.Embeddium", "options"},
                {"me.jellysquid.mods.sodium.client.SodiumClientMod", "options"}
        })
                .add(1, "quality.weatherQuality")
                .add(2, "quality.leavesQuality")
                .add(3, "quality.enableVignette")
                .add(4, 0, 64, "performance.chunkBuilderThreads")
                .add(5, "performance.alwaysDeferChunkUpdates")
                .add(6, "performance.chunkBuildDeferMode")
                .add(7, "performance.animateOnlyVisibleTextures")
                .add(8, "performance.useEntityCulling")
                .add(9, "performance.useFogOcclusion")
                .add(10, "performance.useBlockFaceCulling")
                .add(11, "performance.useNoErrorGLContext")
                .add(12, "advanced.enableMemoryTracing")
                .add(13, "advanced.useAdvancedStagingBuffers")
                .add(14, 0, 9, "advanced.cpuRenderAheadLimit")
                .add(15, "debug.terrainSortingEnabled")
                .add(16, "performance.quadSplittingMode")
                .add(17, "quality.useClosestPointEntitySort")
                .add(18, "performance.useCompactVertexFormat")
                .add(19, "performance.useTranslucentFaceSorting")
                .add(20, "performance.useRenderPassOptimization")
                .add(21, "quality.useQuadNormalsForShading")
                .add(22, "advanced.disableIncompatibleModWarnings");
    }

    /** Sodium Extra / Embeddium (Rubidium) Extra（頭文字 se）。 */
    public static ReflectCodes sodiumExtra() {
        return new ReflectCodes("se", "minecrafttweak.code.provider.se", false, new String[][]{
                {"me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod", "options"}
        })
                // アニメーション
                .add(1, "animationSettings.animation")
                .add(2, "animationSettings.water")
                .add(3, "animationSettings.lava")
                .add(4, "animationSettings.fire")
                .add(5, "animationSettings.portal")
                .add(6, "animationSettings.blockAnimations")
                .add(7, "animationSettings.sculkSensor")
                // パーティクル
                .add(8, "particleSettings.particles")
                .add(9, "particleSettings.rainSplash")
                .add(10, "particleSettings.blockBreak")
                .add(11, "particleSettings.blockBreaking")
                // 細かい描画
                .add(12, "detailSettings.sky")
                .add(13, "detailSettings.sun")
                .add(14, "detailSettings.moon")
                .add(15, "detailSettings.stars")
                .add(16, "detailSettings.rainSnow")
                .add(17, "detailSettings.biomeColors")
                .add(18, "detailSettings.skyColors")
                // 描画
                .add(19, 0, 33, "renderSettings.fogDistance", "renderSettings.fogSettings.atmospheric.distanceChunks")
                .add(20, 0, 100, "renderSettings.fogStart", "renderSettings.fogSettings.atmospheric.startPercent")
                .add(21, "renderSettings.multiDimensionFogControl", "renderSettings.fogSettings.multiDimensionFogControl")
                .add(22, "renderSettings.lightUpdates")
                .add(23, "renderSettings.itemFrame")
                .add(24, "renderSettings.armorStand")
                .add(25, "renderSettings.painting")
                .add(26, "renderSettings.piston")
                .add(27, "renderSettings.beaconBeam")
                .add(28, "renderSettings.limitBeaconBeamHeight")
                .add(29, "renderSettings.enchantingTableBook")
                .add(30, "renderSettings.itemFrameNameTag")
                .add(31, "renderSettings.playerNameTag")
                // その他
                .add(32, "extraSettings.overlayCorner")
                .add(33, "extraSettings.textContrast")
                .add(34, "extraSettings.showFps")
                .add(35, "extraSettings.showFPSExtended")
                .add(36, "extraSettings.showCoords")
                .add(37, "extraSettings.reduceResolutionOnMac")
                .add(38, "extraSettings.useAdaptiveSync")
                .add(39, -64, 1024, "extraSettings.cloudHeight")
                .add(40, 0, 1000, "extraSettings.cloudDistance")
                .add(41, "extraSettings.toasts")
                .add(42, "extraSettings.advancementToast")
                .add(43, "extraSettings.recipeToast")
                .add(44, "extraSettings.systemToast")
                .add(45, "extraSettings.tutorialToast")
                .add(46, "extraSettings.instantSneak")
                .add(47, "extraSettings.preventShaders")
                .add(48, "extraSettings.steadyDebugHud")
                .add(49, 1, 20, "extraSettings.steadyDebugHudRefreshInterval")
                .add(50, "extraSettings.cloudHeightOverride")
                .add(51, "renderSettings.fogSettings.atmospheric.shapeMode")
                .add(52, 0, 100, "renderSettings.fogSettings.atmospheric.cloudFogPercent")
                .add(53, "renderSettings.fogSettings.advanced")
                .add(54, "extraSettings.paniniProjection")
                .add(55, 0, 100, "extraSettings.paniniProjectionStrength");
    }
}
