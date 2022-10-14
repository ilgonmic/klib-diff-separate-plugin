import org.gradle.api.provider.Property

interface KlibDiffExtension {
    val historyLimit: Property<Int>

    companion object {
        const val NAME = "klibDiff"
    }
}
