###########################################################################
#
#  File:     qcmg_util_util.t
#  Creator:  John Pearson
#  Created:  2013-09-03
#
#  $Id: qcmg_util_util.t 4228 2013-09-03 08:01:31Z j.pearson $
#
###########################################################################

use Test::More tests => 7;

BEGIN {
    use_ok('QCMG::Util::Util', qw( qcmgschema_new_to_old
                                   qcmgschema_old_to_new) );
};

# Test qcmgschema field name conversions (4)

ok( qcmgschema_new_to_old('parent_project') eq 'Project', 'convert new to old project' );
ok( qcmgschema_old_to_new('Project') eq 'parent_project', 'convert old to newproject' );
ok( qcmgschema_new_to_old('failed_qc') eq 'Failed QC', 'convert new to old failed_qc' );
ok( qcmgschema_old_to_new('Failed QC') eq 'failed_qc', 'convert old to failed_qc' );
ok( ! defined qcmgschema_old_to_new('stuff'), 'invalid old returns undef' );
ok( ! defined qcmgschema_new_to_old('stuff'), 'invalid new returns undef' );
