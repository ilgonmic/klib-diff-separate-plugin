import org.gradle.api.provider.Property

interface KlibDiffExtension {
    val threshold: Property<Int>

    companion object {
        const val NAME = "klibDiff"
    }
}