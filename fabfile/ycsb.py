from fabric.api import run, roles, env, settings, cd
from fabric.contrib.console import confirm

from datetime import datetime, timedelta

import sys, os
sys.path.append(os.path.dirname(__file__) + '/../conf/')
import hosts, workloads, databases

totalclients = len(env.roledefs['client'])
clientno = 0

timestamp = datetime.now(hosts.timezone).replace(second=0, microsecond=0) + timedelta(minutes=2)
print timestamp

def _getdb(database):
    if not databases.databases.has_key(database):
        raise Exception("unconfigured database '%s'" % database)
    return databases.databases[database]

def _getworkload(workload):
    if not workloads.workloads.has_key(workload):
        raise Exception("unconfigured workload '%s'" % workload)
    return workloads.workloads[workload]

def _outfilename(databasename, workloadname, extension):
    global timestamp
    timestampstr = timestamp.strftime('%Y-%m-%d_%H-%M')
    return '%s_%s_%s.%s' % (timestampstr, databasename, workloadname, extension)

def _at(cmd, time=timestamp):
    return 'echo "%s" | at %s today' % (cmd, time.strftime('%H:%M'))

def _ycsbloadcmd(database, clientno):
    cmd = workloads.root + '/bin/ycsb load -s'
    for (key, value) in database['properties'].items():
        cmd += ' -p %s=%s' % (key, value)
    for (key, value) in workloads.data.items():
        cmd += ' -p %s=%s' % (key, value)
    insertcount = workloads.data['recordcount'] / totalclients
    insertstart = insertcount * clientno
    cmd += ' -p insertstart=%s' % insertstart
    cmd += ' -p insertcount=%s' % insertcount
    outfile = _outfilename(database['name'], 'load', 'out')
    errfile = _outfilename(database['name'], 'load', 'err')
    cmd += ' > %s/%s' % (database['home'], outfile)
    cmd += ' 2> %s/%s' % (database['home'], errfile)
    return cmd

@roles('client')
def load(db):
    global clientno
    database = _getdb(db)
    with cd(database['home']):
        run(_at(_ycsbloadcmd(database, clientno)))
        #run(_at('logger LOAD'))
    clientno += 1

@roles('client')
def kill():
    with settings(warn_only=True):
        run('ps -f -C java')
        if confirm("Do you want to kill Java on the client?"):
            run('killall java')