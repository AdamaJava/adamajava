BEGIN {
  FS  = ",";
  OFS = ",";
}

{
  split( $8, B, " " );
  print $1,$4,B[1],"",$6
}

