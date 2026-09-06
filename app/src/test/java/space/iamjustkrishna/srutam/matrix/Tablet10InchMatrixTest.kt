package space.iamjustkrishna.srutam.matrix

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-mdpi", sdk = [34])
class Tablet10InchMatrixTest : BaseScreenMatrixTest("tablet-10inch")
