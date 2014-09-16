#!/usr/bin/perl

use strict;
#use Data::Dumper;
use Getopt::Long;

my @to;	# allow multiple email addresses
my $body;	
my $subject;

&GetOptions(
		"t=s"	=> \@to,
		"s=s"	=> \$subject,
		"b=s"	=> \$body
	);

if(! defined $to[0]) {
	push @to, 'QCMG-InfoTeam@imb.uq.edu.au';	# default
}
my $email	= join ",", @to;

# requires editing /etc/mail/sendmail.mc:
# dnl define(`SMART_HOST',`smtp.your.provider')dnl -> 
#     define(`SMART_HOST',`smtp.imb.uq.edu.au')
# then recompiling sendmail, allegedly

my $fromemail	= 'mediaflux@qcmg-clustermk2.imb.uq.edu.au';
my $sendmail	= '/usr/sbin/sendmail';

# echo "To: l.fink@imb.uq.edu.au Subject: BWA MAPPING -- COMPLETED\nMAPPING OF 120523_SN7001240_0047_BD12NAACXX.lane_1.nobc has ended. See log file for status: /panfs/seq_raw//120523_SN7001240_0047_BD12NAACXX/log/120523_SN7001240_0047_BD12NAACXX.lane_3.nobc_sam2bam_out.log" |/usr/sbin/sendmail -v -fmediaflux@qcmg-clustermk2.imb.uq.ed.au l.fink@imb.uq.edu.au

my $to		= "To: ".$email;
my $subj	= "Subject: ".$subject;
my $cmd		= qq{echo "$to\n$subj\n$body" | /usr/sbin/sendmail -v -f$fromemail $email}; #"
`$cmd`;

#print STDERR qq{$to $subj $fromemail $email\n};
#print STDERR "$cmd\n";

exit(0);
