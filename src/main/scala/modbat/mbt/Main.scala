package modbat.mbt

import modbat.config.ConfigMgr
import modbat.config.Version
import modbat.log.Log

object Main {
  val config = new Configuration()

  def main(args: Array[String]) {
    Modbat.isUnitTest = false
    System.exit(run(args))
  }

  def run(args: Array[String]): Int = {
    var modelClassName: String = null
    val c = new ConfigMgr("scala modbat.jar", "CLASSNAME",
			  config, new Version ("modbat.mbt"))
    /* delegate parsing args to config library */
    try {
      val remainder = c.parseArgs(args)
      remainder match {
        case Some(remainingArgs) => {
	  if (!remainingArgs.hasNext) {
	    Log.error(c.header)
	    Log.error("Model class argument missing. Try --help.")
	    return 1
	  }
	  modelClassName = remainingArgs.next
	  if (remainingArgs.hasNext) {
	    Log.error("Extra arguments starting at \"" + remainingArgs.next() +
		      "\" are not supported.")
	    return 1
	  }
	}
	case None => // nothing
      }
    } catch {
      case e: IllegalArgumentException => {
	Log.error(e.getMessage())
	return 1
      }
    }

    // If there is a failure during setup, exit the program
    if(setup(modelClassName) == 1) {
      return 1
    } // TODO: refactor into case code below once needed

    Modbat.init
    /* execute */
    val _ret = config.mode match {
      case "dot" =>
        val mbt_launch = MBT.launch(null)
        // if MBT.launch fails return exit code.
        if(mbt_launch._2 == 1) {
          return 1
        }
	new Dotify(mbt_launch._1, modelClassName + ".dot").dotify()
      case _ => Modbat.explore(config.nRuns)
    }

    return _ret
    // TODO (issue #27): Dotify.dotify() and Modbat.explore() should use return code
  }

  def setup(modelClassName: String) = {
    /* configure components */
    Log.setLevel(config.logLevel)
    MBT.enableStackTrace = config.printStackTrace
    MBT.maybeProbability = config.maybeProbability

    MBT.configClassLoader(config.classpath)
    MBT.setRNG(config.randomSeed)
    MBT.isOffline = false
    MBT.runBefore = config.setup
    MBT.runAfter = config.cleanup
    MBT.precondAsFailure = config.precondAsFailure
    MBT.loadModelClass(modelClassName)
  }
}
