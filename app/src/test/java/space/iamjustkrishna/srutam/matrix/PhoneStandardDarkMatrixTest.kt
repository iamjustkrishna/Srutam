package space.iamjustkrishna.srutam.matrix

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Same screens as the light phone matrix, rendered with the system in night mode to catch hardcoded light colours. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-night-420dpi", sdk = [34])
class PhoneStandardDarkMatrixTest : BaseScreenMatrixTest("phone-standard-dark")
