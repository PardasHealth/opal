#!/bin/sh
# prerm script for opal
#
# see: dh_installdeb(1)

set -e

# summary of how this script can be called:
#        * <prerm> `remove'
#        * <old-prerm> `upgrade' <new-version>
#        * <new-prerm> `failed-upgrade' <old-version>
#        * <conflictor's-prerm> `remove' `in-favour' <package> <new-version>
#        * <deconfigured's-prerm> `deconfigure' `in-favour'
#          <package-being-installed> <version> `removing'
#          <conflicting-package> <version>
# for details, see http://www.debian.org/doc/debian-policy/ or
# the debian-policy package

NAME=opal

case "$1" in

  0)
    chkconfig --del opal

    # Read configuration variable file if it is present
    [ -r /etc/default/$NAME ] && . /etc/default/$NAME

    if which service >/dev/null 2>&1; then
            service opal stop
    elif which invoke-rc.d >/dev/null 2>&1; then
            invoke-rc.d opal stop
    else
            /etc/init.d/opal stop
    fi

    # TODO once purge/upgade is implemented uncomment the following

#    EXPORT_FILE=$OPAL_HOME/data/orientdb/opal-config.export
#    echo "prerm migration check ..."
#    JAR_CMD="java -jar /usr/share/opal-*/tools/lib/opal-config-migrator-*-cli.jar"
#    $JAR_CMD --check $OPAL_HOME/data/orientdb/opal-config && \
#    { $JAR_CMD $OPAL_HOME/data/orientdb/opal-config $EXPORT_FILE  && \
#    echo "prerm legacy export completed ..." && \
#    mv $OPAL_HOME/data/orientdb/opal-config $OPAL_HOME/data/orientdb/opal-config.bak || exit 1; }
  ;;

  *)
    echo "prerm called with unknown argument \`$1'" >&2
    exit 1
  ;;
esac


exit 0
