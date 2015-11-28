package edu.cmu.cs.varex

import de.fosd.typechef.featureexpr.FeatureExprFactory
import org.junit.Test

/**
  * Created by ckaestne on 11/27/2015.
  */
class VTest extends AbstractPhpTest {

    val foo = FeatureExprFactory.createDefinedExternal("foo")
    val bar = FeatureExprFactory.createDefinedExternal("bar")

    @Test
    def testBasicValues() {
        eval("") to "";

        eval("echo 'foo';") to "foo";


    }

    @Test
    def testCreateV() {
        eval("echo create_conditional('foo');") to c(foo, "1")
        eval("echo 1+create_conditional('foo');") to c(foo, "2") + c(foo.not(), "1")
    }

    @Test
    def testCreate2V() {
        eval("echo create_conditional('foo'); echo create_conditional('bar');") to c(foo, "1") + c(bar, "1")
        eval("echo 1+create_conditional('foo') + create_conditional('bar');") to c(foo.xor(bar), "2") + c(foo.and(bar), "3") + c(foo.not().and(bar.not()), "1")
    }

    @Test
    def testVConditionalExpr() {
        eval("echo create_conditional('foo')?'x':'y';") to c(foo, "x") + c(foo.not(), "y")
    }

    @Test
    def testVIf() {
        eval("if (create_conditional('foo')) echo 'x'; else echo 'y'; echo 'z';") to c(foo, "x") + c(foo.not(), "y")+"z"
        eval("if (create_conditional('foo')) echo 'x'; else " +
            "if (create_conditional('bar')) echo 'y'; echo 'z';") to c(foo, "x") + c(foo.not().and(bar), "y")+"z"
        eval("if (create_conditional('foo')) echo create_conditional('foo');") to c(foo, "1")
        eval("if (create_conditional('foo')) echo 1+create_conditional('bar');") to c(foo.and(bar), "2") + c(foo.andNot(bar), "1")
    }

    @Test
    def testVVar() {
        eval("$x = 1+create_conditional('foo'); echo 1+$x;") to c(foo, "3") + c(foo.not(), "2")
    }


}
