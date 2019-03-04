import java.io.File
/*
  * Created by oliverh on 7/09/2016.
  */
  object CreateQ3IndelIni {
    def main(args: Array[String]) {

      lazy val version: String = "$Id: CreateQ3IndelIni.scala 11325 2019-02-12 05:30:32Z genomei_prod $".replaceAll("\\$", "")
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
          pw.println("[IOs]")
          (m get 'ref).foreach(v => pw.println(s"ref = $v"))
          (m get 'testBam).foreach(v => pw.println(s"testBam = $v"))
          (m get 'controlBam).foreach(v => pw.println(s"controlBam = $v"))
          (m get 'testVcf).foreach(v => pw.println(s"testVcf = $v"))
          (m get 'controlVcf).foreach(v => pw.println(s"controlVcf = $v"))
          (m get 'out).foreach(v => pw.println(s"output = $v/${(m get 'uuid).get}.vcf.gz"))

          //ids
          pw.println("\n[ids]")
          (m get 'donor).foreach(v => pw.println(s"donorId = $v"))
          (m get 'uuid).foreach(v => pw.println(s"analysisId = $v"))
          (m get 'testSample).foreach(v => pw.println(s"testSample = $v"))
          (m get 'controlSample).foreach(v => pw.println(s"controlSample = $v"))

          // params
          pw.println("\n[parameters]")
          (m get 'mode).foreach(v => pw.println(s"runMode = $v"))
          (m get 'query).foreach(v => pw.println(s"filter = $v"))
          pw.println("threadNo=5\nwindow.nearbyIndel=3\nwindow.homopolymer=100,10\nwindow.softClip =13\n#maximum variant event allow in a strong supporting reads\nstrong.event=3")


          // rules
          pw.println("\n[rules]\n#mark as Somatic except the normal bam provide:\n#novel starts more than gematic.nns\ngematic.nns=2\n#mark as Somatic except the percentage of suppotive reads of informativ one is more than gematic.soi\ngematic.soi=0.05")

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
                     strict:   Boolean = false
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
