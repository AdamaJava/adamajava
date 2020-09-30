# qmule SubSample mode

## Usage

~~~~{.text}
qmule org.qcmg.qmule.SubSample -i <input> -o <output> --proportion <real_number> --log <log>
~~~~

## Options

~~~~{.text}
--help, -h     Show this help message.
--input, -i    Input BAM file.
--log          Log file.
--loglevel     Log level [EXEC,TOOL,DEBUG,INFO], Def=INFO.
--output, -o   Output BAM file.
--proportion   The proportion of total reads you want to output - a real number in the range 0-1.
~~~~
