
BEGIN {
  FS = ",";
  OFS = ",";
}

{
  split( $10, B, " " );
  print $1, $3, B[1], "", $4; 
}
