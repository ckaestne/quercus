package edu.cmu.cs.varex

import com.caucho.quercus.env._
import de.fosd.typechef.featureexpr.{FeatureExpr, FeatureExprFactory}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests vor the EnvVar class (values, vars, and envvar)
  */
@RunWith(classOf[JUnitRunner])
class VEnvVarImplTest extends FlatSpec with Matchers {

    val foo = FeatureExprFactory.createDefinedExternal("foo")
    val bar = FeatureExprFactory.createDefinedExternal("bar")
    var t = FeatureExprFactory.True

    val x = StringValue.create("x")
    val y = StringValue.create("y")

    "Value" should "be nonvariational" in {
        val x2 = StringValue.create("x")

        y.toString() should be("y")
        x.toString() should be("x")
        x should equal( x2)
    }

    "Var" should "be variational" in {
        val v1 = new VarImpl()

        v1.getValue should equal(V.one(NullValue.create()))

        val v2 = new VarImpl(V.one(x))
        v2.getValue should equal(V.one(x))

        val v3 = new VarImpl(V.choice(foo, x, y))
        v3.getValue should equal(V.choice(foo, x, y))
    }

    it should "be conditionally updateable" in {
        val v1 = new VarImpl()

        v1.set(foo, V.one(x))
        v1.getValue should equal (V.choice(foo, x, NullValue.create()))

        v1.set(foo, V.choice(bar, x, y))
        v1.getValue should equal (V.choice(foo, V.choice(bar, x, y), V.one(NullValue.create())))

    }

    "EnvVar" should "hold conditional Vars" in {
        val v1 = new VarImpl()
        val v2 = new VarImpl(V.one(x))

        val e1 = new EnvVarImpl(V.one(v1))
        e1.getValue should equal (V.one(NullValue.create()))
        e1.getVar should equal (V.one(v1))

        val e2 = new EnvVarImpl(V.choice(foo, v1, v2))
        e2.getValue should equal (V.choice(foo, NullValue.create(), x))
        e2.getVar should equal (V.choice(foo, v1, v2))
    }

    it should "be conditionally updateable on same var" in {
        val v1 = new VarImpl()

        val e1 = new EnvVarImpl(V.one(v1))
        e1.set(foo, V.one(x))
        e1.getValue should equal (V.choice(foo.not, NullValue.create(), x))
        e1.getVar should be (V.one(v1))

        val e2 = new EnvVarImpl(V.one(v1))
        e2.set(foo, V.choice(bar, x, y))
        e2.getValue should equal (V.choice(foo.not, V.one(NullValue.create()), V.choice(bar, x, y)))
        e2.getVar should be (V.one(v1))
    }

    it should "share values through vars" in {
        val v1 = new VarImpl()

        val e1 = new EnvVarImpl(V.one(v1))
        val e2 = new EnvVarImpl(V.one(v1))
        e1.set(foo, V.one(x))
        e1.getValue should equal (V.choice(foo.not, NullValue.create(), x))
        e2.getValue should equal (V.choice(foo.not, NullValue.create(), x))

    }
    it should "allow assign by reference" in {
        val v1 = new VarImpl()

        val e3 = new EnvVarImpl(V.one(v1))
        val e4 = new EnvVarImpl(V.one(new VarImpl()))
        e4.setRef(t,e3.getVar)
        e3.set(foo, V.one(x))
        e3.getValue should equal (V.choice(foo.not, NullValue.create(), x))
        e4.getValue should equal (V.choice(foo.not, NullValue.create(), x))
    }

    it should "allow assign by reference conditionally" in {
        val v1 = new VarImpl()
        val v2 = new VarImpl()

        val e3 = new EnvVarImpl(V.one(v1))
        val e4 = new EnvVarImpl(V.one(v2))
        e3.getVar should equal (V.one(v1))
        e4.setVar(bar,e3.getVar)
        e4.getVar should equal (V.choice(bar, v1, v2))
        e3.getVar should equal (V.one(v1))
        e3.set(foo, V.one(x))
        e3.getValue should equal (V.choice(foo.not, NullValue.create(), x))
        e4.getValue should equal (V.choice(foo and bar, x, NullValue.create()))
    }

    it should "update alternative vars" in {
        val v1 = new VarImpl(V.one(x))
        val v2 = new VarImpl(V.one(y))

        val e = new EnvVarImpl(V.choice(foo, v1, v2))
        e.set(bar, V.one(x))
        e.getVar should equal (V.choice(foo, v1, v2))

        v1.getValue should equal (V.one(x))
        v2.getValue should equal (V.choice(foo.not and bar, x, y))
        e.getValue should equal (V.choice(foo or bar, x, y))
    }


    it should "correctly handle setRef conditionally" in {
        val v1 = new VarImpl(V.one(x))
        val v2 = new VarImpl(V.one(y))

        val e1 = new EnvVarImpl(V.one(v1))
        val e2 = new EnvVarImpl(V.one(v2))
        e1.getValue should equal (V.one(x))
        e2.getValue should equal (V.one(y))

        e2.setRef(foo, V.choice(bar, y, v1))

        e1.getValue should equal (V.one(x))
        e2.getVar should equal (V.choice(foo andNot bar, v1, v2))
        e2.getValue should equal (V.choice(foo andNot bar, x, y))

        e2.setRef(foo, V.choice(bar, x, v1))
        assertEquiv(e2.getVar, V.choice(foo andNot bar, v1, v2))
        v1.getValue should equal (V.one(x))
        assertEquiv(v2.getValue, V.choice(foo and bar, x, y))
        assertEquiv(e2.getValue, V.choice(foo.not, y, x))

        v1.set(t, V.one(y))
        v2.set(t, V.one(x))

        e1.getValue should equal (V.one(y))
        assertEquiv(e2.getValue, V.choice(foo andNot bar, y, x))
    }




    //    @Test def testEnvVar(): Unit = {
    //
    //        val v = new VarImpl()
    //        val x = StringValue.create("x")
    //        val y = StringValue.create("y")
    //        v.set(x)
    //        //e and f are variables
    //        val e = new EnvVarImpl(V.one(v))
    //        val f = new EnvVarImpl(V.one(new VarImpl()))
    //        f.setRef(t, e.getVar(t))
    //
    //        println(e.get(t))
    //
    //        assert(e.getVar(t) == V.one(v))
    //        assert(e.get(t) == V.one(x))
    //        assert(f.get(t) == V.one(x))
    //        assert(e.get(t).when(asJavaPredicate((e: Value) => e.toString() == "x")).isTautology())
    //
    //
    //        //assign to new value
    //        e.set(t, V.one(y))
    //        println(e.get(t))
    //
    //        assert(e.getVar(t) == V.one(v))
    //        assert(e.get(t) != V.one(x))
    //        assert(e.get(t) == V.one(y))
    //        assert(f.get(t) == V.one(y))
    //        assert(e.get(t).when(asJavaPredicate((e: Value) => e.toString() == "y")).isTautology())
    //
    //        //assign to new var
    //        val v2 = new VarImpl()
    //        v2.set(x)
    //        e.setVar(t, V.one(v2))
    //
    //        assertEquals(V.one(v2), e.getVar(t))
    //        assert(e.getVar(t) != V.one(v))
    //        assert(e.get(t) == V.one(x))
    //        //keep old value
    //        assert(f.get(t) == V.one(y))
    //
    //    }
    //
    //    @Test def testVEnvVar(): Unit = {
    //
    //        val v = new VarImpl()
    //        val x = StringValue.create("x")
    //        val y = StringValue.create("y")
    //        v.set(x)
    //        val e = new EnvVarImpl(V.one(v))
    //
    //        e.set(foo, V.one(y))
    //
    //        println(e.get(t))
    //
    //        assertEquals(V.one(y), e.getVar(foo))
    //        assertEquals(V.one(x), e.getVar(foo.not()))
    //
    //
    //    }


    def assertEquiv(actual: V[_ <: Any], expected: V[_ <: Any]) = {
        VHelper.smapAll(FeatureExprFactory.True, actual, expected, new Function4[FeatureExpr, Any, Any, Boolean] {
            override def apply(ctx: FeatureExpr, a: Any, b: Any): Boolean = {
                assert(ctx.isContradiction || a == b, s"$b expected but $a found under condition $ctx")
                true
            }
        });
    }


}
