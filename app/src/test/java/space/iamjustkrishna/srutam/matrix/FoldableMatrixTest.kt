package space.iamjustkrishna.srutam.matrix

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w673dp-h841dp-420dpi", sdk = [34])
class FoldableMatrixTest : BaseScreenMatrixTest("foldable")
