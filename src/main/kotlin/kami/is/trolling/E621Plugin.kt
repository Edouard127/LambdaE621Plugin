package kami.`is`.trolling

import com.lambda.client.plugin.api.Plugin

internal object E621Plugin : Plugin() {

    override fun onLoad() {
        hudElements.add(E621Hud)
    }

    override fun onUnload() {
        E621Hud.resources.clear()
    }
}