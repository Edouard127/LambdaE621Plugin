package kami.`is`.trolling

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.lambda.client.plugin.api.PluginHudElement
import com.lambda.client.util.graphics.GlStateUtils
import com.lambda.client.util.graphics.VertexHelper
import com.lambda.client.util.math.Vec2d
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.runSafe
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import javax.imageio.ImageIO

internal object E621Hud : PluginHudElement(
    name = "E621",
    category = Category.MISC,
    description = "uwuwuwuwu",
    pluginMain = E621Plugin
) {
    private val login by setting("username", "")
    private val apiKey by setting("API Key", "")
    private val pixelRange by setting("Pixel Priority", 250, 50..1000, 25)
    private var makePost by setting("Make post", false)
    private var shift by setting("Shift", false)
    private var shiftCount = 0
    var resources: MutableList<Triple<ResourceLocation, Double, Double>> = mutableListOf()
    private var textures: MutableList<Triple<BufferedImage, Double, Double>> = mutableListOf()
    private var readyFlag = false
    override var hudWidth: Float = 100f
    override var hudHeight: Float = 100f

    override fun renderHud(vertexHelper: VertexHelper) {
        super.renderHud(vertexHelper)
        if (shift) {
            shift = false
            shiftCount++
        }
        if (makePost) {
            makePost = false
            getPosts()
        }

        if (readyFlag) {
            readyFlag = false
            textures.forEach {
                resources.add(Triple(
                    mc.textureManager.getDynamicTextureLocation("porn", DynamicTexture(it.first)),
                    it.second, it.third
                ))
            }
            textures.clear()
        }

        runSafe {
            if (resources.isNotEmpty()) {
                // modulus to make it loop
                val index = shiftCount % resources.size
                draw(resources[index])
            }
        }
    }

    private fun getPosts() {
        Thread {
            resources.clear()
            MessageSendHelper.sendChatMessage("Getting posts...")
            val url = URL("https://e621.net/posts.json?limit=20")
            val http: HttpURLConnection = url.openConnection() as HttpURLConnection
            http.setRequestProperty("Content-Type", "application/json")
            http.requestMethod = "GET"
            val response = http.inputStream.bufferedReader().use { it.readText() }
            val postsArray = JsonParser().parse(response).asJsonObject.getAsJsonArray("posts")
            val validPosts = JsonArray()
            postsArray.forEach {
                val ext = it.asJsonObject.get("file").asJsonObject.get("ext").asString
                if (ext == "png") {
                    validPosts.add(it)
                }
            }
            validPosts.forEach {
                val file = it.asJsonObject.get("file").asJsonObject
                val url = file.get("url").asString
                val width = file.get("width").asDouble
                val height = file.get("height").asDouble
                val image = ImageIO.read(URL(url))
                textures.add(Triple(image, width, height))
            }
            MessageSendHelper.sendChatMessage("Finished getting posts!")
            readyFlag = true
        }.start()
    }

    private fun draw(data: Triple<ResourceLocation, Double, Double>) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        GlStateUtils.texture2d(true)

        val width = data.second
        val height = data.third

        val scale = if (width > height) pixelRange / width else pixelRange / height
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        val widthDiff = scaledWidth / width
        val heightDiff = scaledHeight / height
        val avgScale = (widthDiff + heightDiff).toFloat() / 2f
        glScalef(avgScale, avgScale, avgScale)
        hudWidth = scaledWidth.toFloat()
        hudHeight = scaledHeight.toFloat()

        mc.renderEngine.bindTexture(data.first)
        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)

        val center = Vec2d(width / 2, height / 2)
        val halfWidth = width / 2
        val halfHeight = height / 2
        buffer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX)
        buffer.pos(center.x - halfWidth, center.y - halfHeight, 0.0).tex(0.0, 0.0).endVertex()
        buffer.pos(center.x - halfWidth, center.y + halfHeight, 0.0).tex(0.0, 1.0).endVertex()
        buffer.pos(center.x + halfWidth, center.y - halfHeight, 0.0).tex(1.0, 0.0).endVertex()
        buffer.pos(center.x + halfWidth, center.y + halfHeight, 0.0).tex(1.0, 1.0).endVertex()
        tessellator.draw()

        GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    }
}