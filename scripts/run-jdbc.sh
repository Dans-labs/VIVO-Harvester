#!/bin/bash

# Copyright (c) 2010 Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the new BSD license
# which accompanies this distribution, and is available at
# http://www.opensource.org/licenses/bsd-license.html
# 
# Contributors:
#     Christopher Haines, Dale Scheppler, Nicholas Skaggs, Stephen V. Williams - initial API and implementation

# Exit on first error
set -e

# Set working directory
HARVESTERDIR=`dirname "$(cd "${0%/*}" 2>/dev/null; echo "$PWD"/"${0##*/}")"`
HARVESTERDIR=$(cd $HARVESTERDIR; cd ..; pwd)

HARVESTER_TASK=example-jdbc

if [ -f scripts/env ]; then
  . scripts/env
else
  exit 1
fi
prep
echo "Full Logging in $HARVESTER_TASK_DATE.log"

BASEDIR=harvested-data/$HARVESTER_TASK
RAWRHDIR=$BASEDIR/rh-raw
RDFRHDIR=$BASEDIR/rh-rdf
CLONEDIR=$BASEDIR/clone
CLONEDBURL=jdbc:h2:$CLONEDIR/store
MODELDIR=$BASEDIR/model
MODELDBURL=jdbc:h2:$MODELDIR/store
MODELNAME=${HARVESTER_TASK}_temp-transfer
SCOREDATADIR=$BASEDIR/score-data
SCOREDATADBURL=jdbc:h2:$SCOREDATADIR/store
SCOREDATANAME=${HARVESTER_TASK}_score-data
TEMPCOPYDIR=$BASEDIR/temp-copy
PREVHARVESTMODEL="http://vivoweb.org/ingest/example/jdbc-fetch"

#scoring algorithms
EQTEST="org.vivoweb.harvester.score.algorithm.EqualityTest"

#matching properties
SUID="http://vivo.sample.edu/ontology/uId"
DEPTID="http://vivo.sample.edu/ontology/deptId"
POSINORG="http://vivoweb.org/ontology/core#positionInOrganization"
POSFORPERSON="http://vivoweb.org/ontology/core#positionForPerson"
POSDEPTID="http://vivo.sample.edu/ontology/positionDeptId"
BASEURI="http://vivoweb.org/harvest/example/jdbc/"

# clear db clone
rm -rf $CLONEDIR

# clone db
$DatabaseClone -X config/tasks/example.databaseclone.xml --outputConnection $CLONEDBURL

#clear old fetches
rm -rf $RAWRHDIR

# Execute Fetch
$JDBCFetch -X config/tasks/example.jdbcfetch.xml --connection $CLONEDBURL -o $TFRH -OfileDir=$RAWRHDIR

# backup fetch
BACKRAW="raw"
backup-path $RAWRHDIR $BACKRAW
# uncomment to restore previous fetch
#restore-path $RAWRHDIR $BACKRAW

# clear old translates
rm -rf $RDFRHDIR

# Execute Translate
$XSLTranslator -i $TFRH -IfileDir=$RAWRHDIR -o $TFRH -OfileDir=$RDFRHDIR -x config/datamaps/example.jdbc-to-vivo.xsl

# backup translate
BACKRDF="rdf"
backup-path $RDFRHDIR $BACKRDF
# uncomment to restore previous translate
#restore-path $RDFRHDIR $BACKRDF

# Clear old H2 transfer model
rm -rf $MODELDIR

# Execute Transfer to import from record handler into local temp model
$Transfer -o $H2MODEL -OmodelName=$MODELNAME -OcheckEmpty=$CHECKEMPTY -OdbUrl=$MODELDBURL -h $TFRH -HfileDir=$RDFRHDIR -n $NAMESPACE

# backup H2 transfer Model
BACKMODEL="model"
backup-path $MODELDIR $BACKMODEL
# uncomment to restore previous H2 transfer Model
#restore-path $MODELDIR $BACKMODEL

SCOREINPUT="-i $H2MODEL -ImodelName=$MODELNAME -IdbUrl=$MODELDBURL -IcheckEmpty=$CHECKEMPTY"
SCOREDATA="-s $H2MODEL -SmodelName=$SCOREDATANAME -SdbUrl=$SCOREDATADBURL -ScheckEmpty=$CHECKEMPTY"
SCOREMODELS="$SCOREINPUT -v $VIVOCONFIG -VcheckEmpty=$CHECKEMPTY $SCOREDATA -t $TEMPCOPYDIR -b $SCOREBATCHSIZE"

# Clear old H2 score data
rm -rf $SCOREDATADIR

# Clear old H2 temp copy
rm -rf $TEMPCOPYDIR

# uncomment to restore previous H2 temp copy Model
BACKPREPEOPLEORGTEMPDATA="prepeopleorg-tempdata"
#restore-path $TEMPCOPYDIR $BACKPREPEOPLEORGTEMPDATA

# Execute Score for People
$Score $SCOREMODELS -n ${BASEURI}person/ -Auid=$EQTEST -Wuid=1.0 -Fuid=$SUID -Puid=$SUID

# backup H2 temp copy Model
backup-path $TEMPCOPYDIR $BACKPREPEOPLEORGTEMPDATA

# Execute Score for Departments
$Score $SCOREMODELS -n ${BASEURI}org/ -AdeptId=$EQTEST -WdeptId=1.0 -FdeptId=$DEPTID -PdeptId=$DEPTID

# backup H2 score data Model
BACKSCOREDATA="scoredata-prepos"
backup-path $SCOREDATADIR $BACKSCOREDATA
# uncomment to restore previous H2 matched Model
#restore-path $SCOREDATADIR $BACKSCOREDATA

# Find matches using scores and rename nodes to matching uri
$Match $SCOREINPUT $SCOREDATA -t 1.0 -r

# backup H2 transfer Model
BACKPOSTPEOPLEDEPTS="postpeopledepts"
backup-path $MODELDIR $BACKPOSTPEOPLEDEPTS
# uncomment to restore previous H2 transfer Model
#restore-path $MODELDIR $BACKPOSTPEOPLEDEPTS

# clear H2 score data Model
rm -rf $SCOREDATADIR

# Clear old H2 temp copy of input
$JenaConnect -Jtype=tdb -JdbDir=$TEMPCOPYDIR -JmodelName=http://vivoweb.org/harvester/model/scoring#inputClone -t

# uncomment to restore previous H2 temp copy Model
BACKPOSTPEOPLEORGTEMPDATA="postpeopleorg-tempdata"
#restore-path $TEMPCOPYDIR $BACKPOSTPEOPLEORGTEMPDATA

# Execute Score for Positions
POSORG="-AposOrg=$EQTEST -WposOrg=0.34 -FposOrg=$POSINORG -PposOrg=$POSINORG"
POSPER="-AposPer=$EQTEST -WposPer=0.34 -FposPer=$POSFORPERSON -PposPer=$POSFORPERSON"
DEPTPOS="-AdeptPos=$EQTEST -WdeptPos=0.34 -FdeptPos=$POSDEPTID -PdeptPos=$POSDEPTID"
$Score $SCOREMODELS -n ${BASEURI}position/ $POSORG $POSPER $DEPTPOS

# backup H2 temp copy Model
backup-path $TEMPCOPYDIR $BACKPOSTPEOPLEORGTEMPDATA

# backup H2 score data Model
BACKSCOREDATA="scoredata-postpos"
backup-path $SCOREDATADIR $BACKSCOREDATA
# uncomment to restore previous H2 matched Model
#restore-path $SCOREDATADIR $BACKSCOREDATA

# Find matches using scores and rename nodes to matching uri
$Match $SCOREINPUT $SCOREDATA -t 1.0 -r

# Clear old H2 temp copy
rm -rf $TEMPCOPYDIR

# clear H2 score data Model
rm -rf $SCOREDATADIR

# Execute ChangeNamespace lines: the -o flag value is determined by the XSLT used to translate the data
CNFLAGS="$SCOREINPUT -v $VIVOCONFIG -VcheckEmpty=$CHECKEMPTY -n $NAMESPACE"
# Execute ChangeNamespace to get unmatched People into current namespace
$ChangeNamespace $CNFLAGS -u ${BASEURI}person/
# Execute ChangeNamespace to get unmatched Departments into current namespace
$ChangeNamespace $CNFLAGS -u ${BASEURI}org/ -e
# Execute ChangeNamespace to get unmatched Positions into current namespace
$ChangeNamespace $CNFLAGS -u ${BASEURI}position/

# backup H2 matched Model
BACKMATCHED="matched"
backup-path $MODELDIR $BACKMATCHED
# uncomment to restore previous H2 matched Model
#restore-path $MODELDIR $BACKMATCHED

# Backup pretransfer vivo database, symlink latest to latest.sql
BACKPREDB="pretransfer"
backup-mysqldb $BACKPREDB
# uncomment to restore pretransfer vivo database
#restore-mysqldb $BACKPREDB

ADDFILE="$BASEDIR/additions.rdf.xml"
SUBFILE="$BASEDIR/subtractions.rdf.xml"

# Find Subtractions
$Diff -m $H2MODEL -MdbUrl=${PREVHARVDBURLBASE}${HARVESTER_TASK}/store -McheckEmpty=$CHECKEMPTY -MmodelName=$PREVHARVESTMODEL -s $H2MODEL -ScheckEmpty=$CHECKEMPTY -SdbUrl=$MODELDBURL -SmodelName=$MODELNAME -d $SUBFILE
# Find Additions
$Diff -m $H2MODEL -McheckEmpty=$CHECKEMPTY -MdbUrl=$MODELDBURL -MmodelName=$MODELNAME -s $H2MODEL -SdbUrl=${PREVHARVDBURLBASE}${HARVESTER_TASK}/store -ScheckEmpty=$CHECKEMPTY -SmodelName=$PREVHARVESTMODEL -d $ADDFILE

# Backup adds and subs
backup-file $ADDFILE adds.rdf.xml
backup-file $SUBFILE subs.rdf.xml

# Apply Subtractions to Previous model
$Transfer -o $H2MODEL -OdbUrl=${PREVHARVDBURLBASE}${HARVESTER_TASK}/store -OcheckEmpty=$CHECKEMPTY -OmodelName=$PREVHARVESTMODEL -r $SUBFILE -m
# Apply Additions to Previous model
$Transfer -o $H2MODEL -OdbUrl=${PREVHARVDBURLBASE}${HARVESTER_TASK}/store -OcheckEmpty=$CHECKEMPTY -OmodelName=$PREVHARVESTMODEL -r $ADDFILE
# Apply Subtractions to VIVO for pre-1.2 versions
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -r $SUBFILE -m
# Apply Additions to VIVO for pre-1.2 versions
$Transfer -o $VIVOCONFIG -OcheckEmpty=$CHECKEMPTY -r $ADDFILE

# Backup posttransfer vivo database, symlink latest to latest.sql
BACKPOSTDB="posttransfer"
backup-mysqldb $BACKPOSTDB
# uncomment to restore posttransfer vivo database
#restore-mysqldb $BACKPOSTDB

# Tomcat must be restarted in order for the harvested data to appear in VIVO
echo $HARVESTER_TASK ' completed successfully'
#/etc/init.d/tomcat stop
#/etc/init.d/apache2 reload
#/etc/init.d/tomcat start