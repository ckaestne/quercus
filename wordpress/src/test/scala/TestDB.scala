import java.io.File
import java.util.logging._

import com.caucho.quercus.TQuercus
import edu.cmu.cs.varex.VHelper
import edu.cmu.cs.varex.vio.VWriteStreamImpl
import net.liftweb.mocks.MockHttpServletRequest

/**
  * Created by ckaestne on 11/26/2015.
  */
object TestDB extends App {

    //   val php= """
    //      |<?php
    //      |$link = mysql_connect('feature.isri.cmu.edu:3306', 'wordpress_test', 'wp$215$Ux');
    //      |if (!$link) {
    //      |    die('Could not connect: ' . mysql_error());
    //      |}
    //      |echo 'Connected successfully';
    //      |mysql_close($link);
    //      |?>
    //    """.stripMargin
    //
    //
    //    TQuercus.mainScript(php, StdoutStream.create(), null, Map[String,String]())

    val log =Logger.getLogger("com.caucho.quercus")
    log.setLevel(Level.ALL)
    val handler = new ConsoleHandler()
    log.addHandler(handler)
    handler.setLevel(Level.WARNING)

    log.fine("test")

    //    Quercus.main(List("wordpress/src/main/webapp/wordpress-4.3.1/index.php").toArray)
    //
    //
    val request = new MockHttpServletRequest()
    val out = new VWriteStreamImpl()
    new TQuercus().executeFile(new File("wordpress/src/main/webapp/wordpress-4.3.1/index.php"), out, request, VHelper.True())
    val phpResult = out.getPlainOutput.trim
    println(phpResult)
}
