
BEGIN {
  FS = "\t";
  OFS = ",";
}

{
  print $2,$8,$9,"",$13
}

