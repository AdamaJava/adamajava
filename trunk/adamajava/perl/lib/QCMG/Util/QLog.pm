package QCMG::Util::QLog;

##############################################################################
#
#  Module:   QCMG::Util:QLog.pm
#  Author:   John V Pearson
#  Created:  2011-09-26
#
#  This module represents an attempt at a generic logging framework for
#  QCMG perl modules and scripts.
#
#  $Id: QLog.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Exporter;
use Time::HiRes qw( gettimeofday );
use POSIX qw( strftime );
use vars qw( $SVNID $REVISION @ISA @EXPORT $QLOGFH @LEVELS );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QLog.pm 4665 2014-07-24 08:54:04Z j.pearson $'
              =~ /\$Id:\s+(.*)\s+/;

@ISA = qw(Exporter);
# Mandatory exports
@EXPORT = qw( qlogprint qlogfile qlogbegin qlogparams qlogend
              qlogthread qlogaddlevel qlogenv );
@LEVELS = qw( DEBUG INFO WARN ERROR FATAL );

BEGIN {
    # Check if logging is already underway and if so, do not disturb it
    if (defined $MAIN::QLOG) {
        carp "QLog is already working\n"
    }
    else {
        # Start off by logging to STDERR as default
        *MAIN::QLOG = *STDERR;
        # We'll trickily use the scalar part of the glob to keep a track
        # of the fact that we are currently using STDERR.  We'll need this
        # trick in qlogfile() to decide whether to close the current log.
        my $file = 'STDERR';
        *MAIN::QLOG = \$file;
        # While we're being tricky we'll park the default values for the
        # label, thread and level in the hash part of the glob
        *MAIN::QLOG = { l=>'INFO', t=>'main', v=>'INFO' };

        # I know it might be better to use *CORE::GLOBAL::die here
        # rather that $SIG handlers and one day I *might* even get around
        # to learning how to do that - JVP.

        # We're also going to register handlers for WARN and DIE
        $SIG{__WARN__} = sub {
            qlogprint( {l => 'WARN'}, @_ );
            if ($MAIN::QLOG ne 'STDERR') {
                print STDERR @_;
            }
        };
        $SIG{__DIE__} = sub {
            my $errno = $! + 0;
            qlogprint( {l => 'FATAL'}, @_ );
            # This doesn't work:
            #qlogprint( {l => 'EXEC'}, "Exit status $errno\n" );
            if ($MAIN::QLOG ne 'STDERR') {
                print STDERR @_;
            }
            exit(1);
            # We don't need to explicity exit. From perldoc perlvar:
            # "When a __DIE__ hook routine returns, the exception 
            #  processing continues as it would have in the absence of
            #  the hook, unless the hook routine itself exits via a 
            #  "goto", a loop exit, or a die()."
        };
    }
}


# Allows you to insert new logging levels into the hierachy
sub qlogaddlevel {
    my $newlevel = shift;
    my $spec     = shift;
    if ($spec =~ /([\>\<]{1})\s*([A-Z]+)/) {
        my $location = $1;
        my $insert_level = $2;
        my @oldlevels = @LEVELS;
        my @newlevels = ();
        while (@oldlevels) {
            my $level = shift @oldlevels;
            if ($level eq $insert_level) {
               if ($location eq '<') {
                   push @newlevels, $newlevel, $level;
               }
               else {
                   push @newlevels, $level, $newlevel;
               }
            }
            else {
                push @newlevels, $level;
            }
        }
        @LEVELS = @newlevels;
    }
    else {
        die "qloglevel parameter $spec is invalid";
    }
}


sub qlogfile {
    my $file = shift;
    my %opts = @_;

    # If the logger is currently glob'd to STDERR do not close it or
    # you'll close STDERR as well!  If the QLOG glob points to a 
    # regular file then close it before opening the new log file.

    if ($MAIN::QLOG ne 'STDERR') {
        close MAIN::QLOG;
    }

    # Default behaviour is to open a new log file such that any existing
    # file of the same name will get overwritten.  m.anderson has a use
    # case where he'd like to append not overwrite so we'll have a crack.
    # If you pass in "append => 1" then we'll try to append.

    if ($opts{append}) {
        open( QLOGFH, ">>$file" ) or 
            croak "Cannot open log file $file for writing: $!";
    }
    else {
        open( QLOGFH, ">$file" ) or 
            croak "Cannot open log file $file for writing: $!";
    }
    select( QLOGFH );
    $|++;  # turn buffering off
    select( STDOUT );  # reset default filehandle to STDOUT
    #print QLOGFH "printing something to QLOGFH prior to globbing\n";

    my %qv = %MAIN::QLOG;  # save these away or the globbing will wipe them

    *MAIN::QLOG = *QLOGFH;
    *MAIN::QLOG = \$file;  # remember the name of the file being logged to
    *MAIN::QLOG = \%qv;    # replace the saved thread and label info
}
 

# Change the default value for the thread
sub qlogthread {
    $MAIN::QLOG{t} = shift;
}


# Change the default value for the logging level
sub qloglevel {
    my $default = shift;
    # Check that the level exists
    foreach my $level (@QCMG::Util::QLog::LEVELS) {
    }
    $MAIN::QLOG{v} = shift;
}


# qlogprint prints to the log at filehandle MAIN::QLOG.  If the first
# parameter is a hashref then it is parsed for non-default label and
# thread identifiers.  Defaults for label and thread are 'INFO' and 'main'.

sub qlogprint {
    my @params = @_;
    my $label  = $MAIN::QLOG{l};
    my $thread = $MAIN::QLOG{t};
    if (ref($params[0]) eq 'HASH') {
        my $param = shift @params;
        $label  = $param->{l} if exists $param->{l};
        $thread = $param->{t} if exists $param->{t};
    }

    # Get caller.  If 'main' then substitute script name and if QLog
    # itself (dir, warn etc) then go back one more calling frame to try
    # to find the originator.  Note that the "class if" must come before
    # the "main if" and that they must be independent and not if-elsif.
    my $class = caller();
    # Crazy regex is to try (so far unsuccessfully) to cope with the case
    # where the code has been through qperlpackage
    if ($class =~ /QCMG[:_]{1,2}Util[:_]{1,2}QLog/) {
        $class = caller(1);
    }
    if ($class eq 'main') {
        $class = $0;
        $class =~ s/^.*\///g;
    }

    my ($seconds, $microseconds) = gettimeofday;
    my $now = strftime( q/%H.%M.%S./, localtime($seconds)) .
              sprintf("%03d", $microseconds/1000); 
    print MAIN::QLOG "$now [$thread] $label $class - ", @params;
}


sub qlogbegin {
    qlogprint( {l=>'EXEC'}, 'StartTime '.localtime()."\n" );
    qlogprint( {l=>'EXEC'}, 'ProcessID '.$$."\n" );
    my $host = `uname -n`;
    $host =~ s/\n//;
    qlogprint( {l=>'EXEC'}, 'Host '.$host."\n" );
    my $user = `id -un`;
    $user =~ s/\n//;
    qlogprint( {l=>'EXEC'}, 'RunBy '.$user."\n" );
    qlogprint( {l=>'EXEC'}, 'RealUID '.$<."\n" );
    qlogprint( {l=>'EXEC'}, 'EffectiveUID '.$>."\n" );
    qlogprint( {l=>'EXEC'}, 'OsName '.$^O."\n" );
    my $osarch = `uname -p`;
    $osarch =~ s/\n//;
    qlogprint( {l=>'EXEC'}, 'OsArch '.$osarch."\n" );
    my $osver = `uname -r`;
    $osver =~ s/\n//;
    qlogprint( {l=>'EXEC'}, 'OsVersion '.$osver."\n" );
    qlogprint( {l=>'EXEC'}, 'PerlVersion '.sprintf("%vd",$^V)."\n" );
    qlogprint( {l=>'EXEC'}, 'PerlExecutable '.$^X."\n" );
    $0 =~ /([^\/]+)$/;
    qlogprint( {l=>'EXEC'}, 'ToolName '.$1."\n" );
    if (defined $::REVISION) {
        qlogprint( {l=>'EXEC'}, 'ToolVersion '.$::REVISION."\n" );
    }
}


sub qlogparams {
    my $rh_params = shift;
    foreach my $key (sort keys %{$rh_params}) { 
        if (ref $rh_params->{$key} eq 'ARRAY') {
            my $num_vals = scalar( @{$rh_params->{$key}} );
            if ( $num_vals > 0 ) {
                foreach my $ctr (0..($num_vals-1)) {
                     qlogprint( {l=>'PARAM'}, "$key ".$rh_params->{$key}->[$ctr]."\n" );
                }
            }
            else {
                qlogprint( {l=>'PARAM'}, "$key\n" );
            }
        }
        else {
            qlogprint( {l=>'PARAM'}, "$key ".$rh_params->{$key}."\n" );
        }
    }
}


sub qlogenv {
    qlogprint({l=>'ENV'},"$_ ".$ENV{$_}."\n") foreach sort keys %ENV;
}


sub qlogend {
    qlogprint( {l=>'EXEC'}, 'StopTime '.localtime()."\n" );
}


1;

__END__


=head1 NAME

QCMG::Util::QLog - QCMG Perl logging framework


=head1 SYNOPSIS

 use QCMG::Util::QLog;

 qlogfile('mylog1.log');
 qlogbegin();
 qlogprint("processing first file\n");
 ...
 qlogend();

=head1 DESCRIPTION

This module is not a class but it provides a framework for logging in
QCMG perl applications.  It exports 5 functions into the main namespace
- qlogfile, qlogprint, qlogthread, qlogbegin and qlogend.

You incorporate this module into your script just as you would any other
perl module - by I<use>ing it.  Logging is activated immediately and
the output of any warn or die statements will be changed to fit the
following pattern:

 timestamp [thread] label - message

For example:

 22.44.02.985 [main] EXEC - StartTime Thu Sep 29 22:44:02 2011
 22.44.02.986 [main] EXEC - Host qcmg-clustermk2.imb.uq.edu.au
 22.44.02.986 [main] EXEC - RunBy jpearson
 22.44.02.986 [main] EXEC - OsName linux
 22.44.02.989 [main] EXEC - OsArch x86_64
 22.44.02.991 [main] EXEC - OsVersion 2.6.18-164.11.1.el5
 22.44.02.991 [main] EXEC - ToolName test_mule.pl
 22.44.02.991 [main] EXEC - ToolVersion 1177
 22.44.02.991 [main] EXEC - PerlVersion 5.8.8

This is not an arbitrary format, it was chosen because it is an exact
copy of the logging format used by the QCMG Adama suite of Java
programs.  The default value for B<thread> is B<main> and the default
value for B<label> is B<INFO>.  Thread is irrelevant in the majority of
perl scripts because they are single-threaded. Also note that there has
been no serious attempt to make this logging framework thread-safe so
don't get too excited about thread.  Label is of more immediate use and
is meant to categorise the log message.  For example, all warnings
generated by calling warn will be written to the log with the label
B<WARN> and any calls to die will generate log messages with the label
B<FATAL>.  Functions B<qlogbegin()> and B<qlogend()> also have a special
label - B<EXEC> to indicate that the log messages they write relate to the
EXECution of the script.  The default INFO label is appropriate for most
progress and diagnostic log messages but the B<TOOL> label can be used
for any critical progress messages that might be useful to parse out,
for example the number of records read by a script that processes reads
from BAM files.

As well as providing some categorisation of log messages, the label
provides a simple mechanism for grep'ing out lines from log files.  For
example, you may wish to push some of the values from EXEC log messages
into a database or you may wish to scan for FATAL lines to work out why
a script exited abnormally.

EXEC log messages have some additional internal structure - the message
string should be a B<name value> pair.  This can be clearly seen from
the examples above where each line has a name (RunBy, OsName etc) and
a value (jpearson, linux etc).

Because logging is activated immediately after the use statement, the
commandline parameters may not have been parsed so a user-specified log
file will not yet be available.  For this reason, logging is 
initially directed to
STDERR.  If you use qlogprint, warn or die, then the output to STDERR
will be in log format but if you I<print STDERR> the output will not be
in log format.  In some cases, logging to STDERR and redirecting the
STDERR may be good enough but if you are intending to keep the log file,
it's probably easier to call B<qlogfile()> to direct the log entries to
a file.

One often-desirable log line is the commandline options but this can be
troublesome if you want to log to a file and are using the Getopt
modules to parse the commandline options.  Getopt reads and empties the
@ARGV array so by the time you know the name of the file that the user
wants to use for logging, the commandline options have dissappeared.  If
you want to capture the commandline options, you could do something
like this:

 my $cmdline = join(' ',@ARGV);
 my $results = GetOptions ( 'l|logfile=s' => \$logfile,
                             ... );
 qlogfile($logfile) if $logfile;
 qlogbegin;
 qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");


=head1 FUNCTIONS

=over

=item B<qlogprint()>

 qlogprint( 'This will be printed to the log' );
 qlogprint( {l=>'TOOL'},
            'This will be printed to the log with label TOOL' );
 qlogprint( {l=>'TOOL',t=>'thread2'},
            'This will be printed to the log with label TOOL and thread2' );

This method takes an array of strings which will be concatenated and
printed to the log filehandle.  A "\n" will not be appended so you will
very probably want to incorporate one into any strings you supply to
qlogprint.  You can have "\n"s in the middle of the strings to be
printed as well as at the end but that would
break the pattern of having the date stamp and other information at the
beginning of every line.  It's up to you but in most cases, you probably
want to pass a single newline-terminated string to qlogprint.
If you need multiple lines of output, multiple qlogprint calls would
certainly look neater.  Your call.

If the first argument to qlogprint is a hashref, the hash will be
examined for values for B<t> and B<l> which will be used as the values
for the B<thread> and B<label> values for this log entry.  Note that the
defaults for thread and label will not be changed.  The default for
label is always B<INFO> and this can never be changed so you will always
have to pass in a hash if you wish to use a label other than INFO.
The default for thread can be changed by calling B<qlogthread()>.

=item B<qlogfile()>

 qlogfile('mylog1.log');
 qlogfile('mylog2.log');
 qlogfile('mylog2.log', append => 1);

Changes the log file.  If the logging has been going to STDERR, STDERR
is returned to normal and logging is redirected to the new file.  If
qlogfile is called multiple times in a script, the existing log file is
closed each time and the new log file opened.  You can only ever have a
single log file open at a time.  Once qlogfile has been called, all
output from warn and die will go to the log file in log format but it
will also be sent to STDERR in non-log format.

Default behaviour is to open a new log file such that any existing
file of the same name will get overwritten.  A use case has come up
where someone would like to append not overwrite so you can now
optionally pass extra parameters to qlogfile to drive its behaviour.
If you pass in "append => 1" then we'll try to append.

=item B<qlogthread()>

 qlogthread( 'thread2' );

While the log format calls for a thread name, there has been no effort
to make this logging framework thread-safe.  It I<may> be thread-safe
but no guarantees.
In any case, calling this function with a string will set the default
thread label for all subsequent qlogprint, qlogbegin, qlogend, warn
and die statements.

=item B<qlogbegin()>

 qlogbegin;

This function causes a series of lines to be written to the log
describing the execution environment including the starttime, the
hostname, user, perl version, OS type and version etc.  It's quite
likely that the exact list of values written will evolve so the best way
to get a current list is to use it in a script.

=item B<qlogparams()>
 
 qlogparams( \%run_paramaters );

This function logs (INFO) the contents of a hass as though they were run
parameters.

=item B<qlogend()>

 qlogend;

Writes the script stoptime to the log.  Note that this value can only be
approximate and to make it as accurate as possible, qlogend should be
called as the final statement in a script or immediately prior to exit
or die statements.

=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: QLog.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT


Copyright (c) John Pearson 2011-2013
Copyright (c) The University of Queensland 2011

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
=cut
