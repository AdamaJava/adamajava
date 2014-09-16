package QCMG::Automation::Common;

##############################################################################
#
#  Module:   QCMG::Automation::Common.pm
#  Creator:  Lynn Fink
#  Created:  2011-03-01
#
#  This class contains common methods for automating the ingest into Mediaflux
#  of raw sequencing  data; intended to be inherited via another QCMG module
#
#  $Id: Common.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
##############################################################################


=pod

=head1 NAME

QCMG::Automation::Common -- Common functions for the automation pipeline 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Automation::Common->new();

=head1 DESCRIPTION

Contains methods for extracting run information from a raw sequencing run
folder, checking the sequencing files, and ingesting them into Mediaflux

=head1 REQUIREMENTS

 Exporter
 POSIX 'strftime'
 QCMG::Automation::Config

=cut

use strict;

# standard distro modules
use Data::Dumper;
use POSIX 'strftime';			# for printing timestamp
use Cwd;

# in-house modules

# get configuration parameters and make accessor methods
use QCMG::Automation::Config;
use vars qw($c);
$c = \%QCMG::Automation::Config::c;
use base qw(Class::Accessor);
QCMG::Automation::Common->mk_accessors(keys %{$c});

use vars qw( $SVNID $REVISION );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 none

Returns:
 a new instance of this class.

=cut

sub new {
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {};
	bless ($self, $class);

	$self->_init_accessors();

	return $self;
}


################################################################################
=pod

B<timestamp()> 
 Generate a timestamp in the format: 2003-02-14-16:37:46
                                     030214
                                     030214163746

Parameters:
 FORMAT 
   - ISO8601       - Default timestamp, ISO 8601 format
   - YYMMDD        - timestamp in YYYYMMDD format 
   - YYMMDDhhmmss  - timestamp in YYYYMMDDhhmmss format 

Returns:
 scalar - timestamp string

=cut
sub timestamp {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print STDERR "TIMESTAMP ".$options->{'FORMAT'}."\n";

	# should be in ISO 8601 format: [2011-02-19 23:59:99Z]
	my $stamp = lc strftime("[%Y-%m-%d %H:%M:%S]", localtime); 

	# return date in YYMMDD format
	if($options->{'FORMAT'} eq 'YYMMDD') {
		$stamp = uc strftime("%Y%m%d", localtime);
	}
	elsif($options->{'FORMAT'} =~ /yymmddhh/i) {
		$stamp = uc strftime("%Y%m%d%H%M%S", localtime);
	}

	return($stamp);
}


################################################################################
=pod

B<checksum()> 
 Generate file checksum

Parameters:
 FILE = path and name of file

Returns:
 checksum CRC, block, filename

=cut
sub checksum {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	# compare two files:
	#cksum ingest_schematic_110218.graffle vars.pl  | perl -ane '$x ||= $F[0]; warn if $x != $F[0];'

	my ($csum) = `cksum $options->{'FILE'}`; 
	my ($crc, $blocks, $fname) = split /\s+/, $csum; 

	return($crc, $blocks, $fname);
}

################################################################################
=pod

B<init_log_file()> 
 Generate a log file in the run folder to output its details to.  If the logfile 
 already exists, it is renamed so it will not be overwritten.  
 If it cannot create a log file, or rename an existing one, it dies.

Parameters:
 LOG_FILE or uses self

Returns:
 scalar - handle to logfile if successful; open> error if not

=cut
sub init_log_file {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$options->{'LOG_FILE'} = $self->{'log_file'} if(!  $options->{'LOG_FILE'});

	my $status = 0;

	my $date = $self->timestamp(FORMAT => 'YYMMDDhhmmss');

	# if user only specified a path, not a log file, fail
	if(-d $options->{'LOG_FILE'}) {
		print STDERR "Log file specified is a directory, not a file.\n";
		print STDERR "Exiting.\n";

		die;
	}

	if(-e $options->{'LOG_FILE'} && -w $options->{'LOG_FILE'}) {
		# create new filename with date of creation
		my $oldlog = $options->{'LOG_FILE'}."_".$date;

		# 1 = success; 0 = fail
		my $rv = rename($options->{'LOG_FILE'}, $oldlog);

		if($rv != 1) {
			print STDERR "Could not rename old log file.\nPlease check file permissions.\n";
			print STDERR "Exiting.\n";

			die;
		}
	}
	elsif(-e $options->{'LOG_FILE'} && ! -w $options->{'LOG_FILE'}) {
		#if cannot write to requested log file, write log file to /tmp
		#$options->{'LOG_FILE'} = "/tmp/qcmg-automation-common.log";
		print STDERR "Could not write old log file.\nPlease check file or file permissions.\n";
		print STDERR "Exiting.\n";

		die;
	}

	open(LOG, ">".$options->{'LOG_FILE'}) || warn "Cannot open $options->{'LOG_FILE'} for writing: $!\n";

	my $log_fh		= *LOG;
	$self->{'log_fh'}	= *LOG;

	return($log_fh);
}


################################################################################
=pod

B<writelog()> 
 Print information to log file

Parameters:
 BODY - what to log to file
 STDERR - print to STDERR as well as file

Returns:
 void

=cut
sub writelog {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $fh = $self->{'log_fh'};

	print $fh $options->{'BODY'}, "\n";

	if($options->{'STDERR'}) {
		print STDERR $options->{'BODY'}, "\n";
	}


        return;
}



################################################################################
=pod

B<execlog()> 

Parameters:
 Write EXECLOG names and values to log file

Returns:

=cut
sub execlog {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# print required log info
	# command line args
	my $cline	= join " ", @ARGV;			
	# perl version as number
	#my $perlver	= join('.', map {ord} split('', $^V)); 	# doesn't work on barrine
	my $perlver	= `perl -version`;
	#This is perl, v5.10.0 built for x86_64-linux-thread-multi
	$perlver	=~ /This is perl, v(.+?)\s/;
	$perlver	= $1;

	my $cwd		= getcwd;

	my $date = $self->timestamp();
	
	my $msg		 = qq{EXECLOG: startTime $date\n};
	$msg		.= qq{EXECLOG: host }.$self->HOSTKEY.qq{\n};
	$msg		.= qq{EXECLOG: runBy }.`whoami`;
	$msg		.= qq{EXECLOG: osName }.`uname -s`;
	$msg		.= qq{EXECLOG: osArch }.`arch`;
	$msg		.= qq{EXECLOG: osVersion }.`uname -v`;
	$msg		.= qq{EXECLOG: toolName $0\n};
	$msg		.= qq{EXECLOG: toolVersion $REVISION\n};
	$msg		.= qq{EXECLOG: cwd $cwd\n};
	$msg		.= qq{EXECLOG: commandLine $0 $self->{'cmdline'}\n};
	$msg		.= qq{EXECLOG: perlVersion $perlver\n};
	$msg		.= qq{EXECLOG: perlHome $^X};

	$self->{'startTime'} = $date;

	$self->writelog(BODY => $msg);

	return();
}

################################################################################
=pod

B<elapsed_time()> 

 Calculate elapsed time between start time and now (ending of script)

Parameters:

Returns:
 elapsed time string

=cut
sub elapsed_time {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	# [28-02-11 16:05:34] - [29-03-11 01:04:58]

	my $end		= $self->timestamp();
	my $start	= $self->{'startTime'};

	#                     D 0    M 1    Y 2   H 3    m 4    s 5
	my $rx		= '\[(\d+)\-(\d+)\-(\d+) (\d+)\:(\d+)\:(\d+)\]';

	my @stimes	= ($start	=~ /$rx/);
	my @etimes	= ($end		=~ /$rx/);

	#print Dumper @stimes;
	#print Dumper @etimes;

	my $Y	= $etimes[0] - $stimes[0];
	my $M	= $etimes[1] - $stimes[1];
	my $D	= $etimes[2] - $stimes[2];
	my $h	= $etimes[3] - $stimes[3];
	my $m	= $etimes[4] - $stimes[4];
	my $s	= $etimes[5] - $stimes[5];

	my $msg;

	$msg .= "$Y years, "	if($Y > 0);
	$msg .= "$M months, "	if($M > 0);
	$msg .= "$D days, "	if($D > 0);
	$msg .= "$h hours, "	if($h > 0);
	$msg .= "$m minutes, "	if($m > 0);
	$msg .= "$s seconds "	if($s > 0);

	return($msg);
}

################################################################################
=pod

B<cmdline()> 
 Log the command line used to call the parent script

Parameters:
 LINE -> string of command line

Returns:

=cut
sub cmdline {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->{'cmdline'} = $options->{'LINE'};

	return;
}

################################################################################
=pod

B<tracklite_connect()> 
 Connect to Tracklite database and return a connection handle

Parameters:

Returns:
 QCMG::DB::Tracklite object -> database connection handle

=cut
sub tracklite_connect {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
	# RUN_FOLDER - directory where runs are
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	require QCMG::DB::TrackLite;

	# for testing; leave empty if real
	my $tl = QCMG::DB::TrackLite->new(
					);
	#use QCMG::DB::TrackLite;
	#my $tl = QCMG::DB::TrackLite->new();
	$tl->connect();

	return($tl);
}

################################################################################
=pod

B<send_email()> 
 Send email to notify of ingestion.
 
Parameters:
 SUBJECT
 BODY
 EMAIL (a single email address)

Returns:
 void

=cut
sub send_email {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	# only works for single address...
	if(! $self->{'email'}) {
		push @{$self->{'email'}}, $options->{'EMAIL'};
	}

	my $emails = join ", ", @{$self->{'email'}};
	$self->writelog(BODY => "Sending email to: $emails") if($self->{'log_file'});

	# requires editing /etc/mail/sendmail.mc:
	# dnl define(`SMART_HOST',`smtp.your.provider')dnl -> 
	#     define(`SMART_HOST',`smtp.imb.uq.edu.au')
	# then recompiling sendmail, allegedly

	my $fromemail	= 'mediaflux@qcmg-clustermk2.imb.uq.edu.au';
	my $sendmail	= '/usr/sbin/sendmail';

	# echo "To: l.fink@imb.uq.edu.au Subject: BWA MAPPING -- COMPLETED\nMAPPING OF 120523_SN7001240_0047_BD12NAACXX.lane_1.nobc has ended. See log file for status: /panfs/seq_raw//120523_SN7001240_0047_BD12NAACXX/log/120523_SN7001240_0047_BD12NAACXX.lane_3.nobc_sam2bam_out.log" |/usr/sbin/sendmail -v -fmediaflux@qcmg-clustermk2.imb.uq.ed.au l.fink@imb.uq.edu.au

	my $to		= "To: ".$emails;
	my $subj	= "Subject: ".$options->{'SUBJECT'};
	my $cmd		= qq{echo "$to\n$subj\n$options->{'BODY'}" | /usr/sbin/sendmail -v -f$fromemail $emails}; #"
	`$cmd`;

        #my $cmd		= qq{echo "To: $emails}."\n".qq{Subject: }.$options->{'SUBJECT'}."\n".$options->{'BODY'}.qq{" | /usr/sbin/sendmail -v -f$fromemail $emails};
	#"
	
	print STDERR qq{$cmd\n};

	return;
}

################################################################################
=pod

B<get_my_ip()> 
 Get IP address of host machine (UNIX only)

Parameters:
 -none-

Returns:
 scalar - IP address

=cut
sub get_my_ip {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }


	#eth0      Link encap:Ethernet  HWaddr 00:10:18:89:2b:48  
	#          inet addr:10.160.72.27 ...

	my $ifcfg = `/sbin/ifconfig`;

	$ifcfg =~ /eth0.+?inet addr:(\d+\.\d+\.\d+\.\d+)/s;

	return($1);
}

################################################################################
=pod

B<get(), set(), _init_accessors()> 
 Define get and set accessor methods to make getting and setting configuration
 parameters easy

Parameters:

Returns:

=cut
sub get {
	my $self = shift;

	#print STDERR "Getting @_\n";

	$self->Class::Accessor::get(@_);
}

sub set {
	my ($self, $key) = splice(@_, 0, 2);

	#print STDERR "Setting $key to @_\n";

	$self->Class::Accessor::set($key, @_);
}

sub _init_accessors {
	my $self = shift;

	foreach my $k (keys %{$c}) {
		$self->$k($c->{$k});
	}
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
