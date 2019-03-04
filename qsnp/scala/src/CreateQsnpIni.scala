import java.io.File
/**
  * Created by oliverh on 7/09/2016.
  */

  object CreateQsnpIni {


    def main(args: Array[String]) {


      lazy val version: String = "$Id: CreateQsnpIni.scala 11325 2019-02-12 05:30:32Z genomei_prod $".replaceAll("\\$", "")
      for (arg <- args if arg.endsWith("version")) {
        println (version)
        System.exit(0)
      }

      // Required positional arguments by key in options
      val required = List('testBam, 'mode, 'ref, 'out, 'donor, 'uuid, 'query, 'iniFile)


      // Options with value
      val optional = Map("--testBam" -> 'testBam, "--testVcf" -> 'testVcf, "--testSample" -> 'testSample,
        "--controlBam" -> 'controlBam, "--controlVcf" -> 'controlVcf, "--controlSample" -> 'controlSample,
        "--mode" -> 'mode, "--ref" -> 'ref, "--out" -> 'out, "--query" -> 'query, "--donor" -> 'donor, "--uuid" -> 'uuid, "--iniFile" -> 'iniFile, "--amplicon" -> 'amplicon)

      // Flags
      val flags: Map[String, Symbol] = Map()

      // Default options that are passed in
      val defaultOptions: Map[Symbol, String] = Map[Symbol, String]()

      // Parse options based on the command line args
      val options = parseOptions(args.toList, required, optional, flags, defaultOptions)
      println(options)

      //TODO check optionsMap contains all entries in required

      writeIni(options, options.get('iniFile).get)
    }

    def writeIni(m: OptionMap, s: String): Unit = {
      if (!new File(s).exists()) {
        println(s"Will write to output file $s")
        val pw = new java.io.PrintWriter(new File(s))
        try {
          // inputs
          pw.println("[inputFiles]")
          (m get 'ref).foreach(v => pw.println(s"ref = $v"))
          (m get 'testBam).foreach(v => pw.println(s"testBam = $v"))
          (m get 'controlBam).foreach(v => pw.println(s"controlBam = $v"))
          (m get 'testVcf).foreach(v => pw.println(s"testVcf = $v"))
          (m get 'controlVcf).foreach(v => pw.println(s"controlVcf = $v"))

          // params
          pw.println("\n[parameters]")
          (m get 'mode).foreach(v => pw.println(s"runMode = $v"))
          (m get 'query).foreach(v => pw.println(s"filter = $v"))
          (m get 'skipAnnotation).foreach(v => pw.println(v))
          (m get 'amplicon).foreach(v => if (v == "true") pw.println("includeDuplicates = true\nnumberNovelStarts = 1"))


          //ids
          pw.println("\n[ids]")
          (m get 'donor).foreach(v => pw.println(s"donor = $v"))
          (m get 'uuid).foreach(v => pw.println(s"analysisId = $v"))
          (m get 'testSample).foreach(v => pw.println(s"testSample = $v"))
          (m get 'controlSample).foreach(v => pw.println(s"controlSample = $v"))

          // output
          pw.println("\n[outputFiles]")
          (m get 'out).foreach(v => pw.println(s"vcf = $v/${(m get 'uuid).get}.vcf.gz"))

          // rules
          pw.println("\n[rules]")
          (m get 'amplicon).fold(pw.println("control1=0,20,3\ncontrol2=21,50,4\ncontrol3=51,,10\ntest1=0,20,3\ntest2=21,50,4\ntest3=51,,5"))(v => if (v == "true") pw.println("control1=10,,10\ntest1=10,,5") else pw.println("control1=0,20,3\ncontrol2=21,50,4\ncontrol3=51,,10\ntest1=0,20,3\ntest2=21,50,4\ntest3=51,,5"))

        }finally pw.close()
      } else println (s"File already exists $s")
    }


    implicit class OptionMapImprovements(val m: Map[String, Symbol]) {
      def match_key(opt: String): String = {
        m.keys.find(_.matches(s""".*$opt(\\|.*)?""")).getOrElse("")
      }

      def match_get(opt: String): Option[Symbol] = {
        m.get(m.match_key(opt))
      }

      def match_apply(opt: String): Symbol = {
        m(m.match_key(opt))
      }
    }


    type OptionMap = Map[Symbol, String]
    type OptionMapBuilder = Map[String, Symbol]
    def parseOptions(args:     List[String],
                     required: List[Symbol],
                     optional: OptionMapBuilder,
                     flags:    OptionMapBuilder,
                     options:  OptionMap = Map[Symbol, String](),
                     strict:
                     Boolean = false
                    ): OptionMap = {
      args match {
        // Empty list
        case Nil => options

        // Options with values
        case key :: value :: tail if optional.match_get(key) != None =>
          parseOptions(tail, required, optional, flags,  options ++ Map(optional.match_apply(key) -> value))

        // Flags
        case key :: tail if flags.match_get(key) != None =>
          parseOptions(tail, required, optional, flags,  options ++ Map(flags.match_apply(key) -> "true"))

        // Positional arguments
        case value :: tail if required != Nil =>
          parseOptions(tail, required.tail, optional, flags,  options ++ Map(required.head -> value))

        // Generate symbols out of remaining arguments
        case value :: tail if !strict => parseOptions(tail, required, optional, flags,  options ++ Map(Symbol(value) -> value))

        case _ if strict =>
          printf("Unknown argument(s): %s\n", args.mkString(", "))
          sys.exit(1)
      }
    }


  }
