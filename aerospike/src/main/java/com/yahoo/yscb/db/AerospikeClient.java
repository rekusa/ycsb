package com.yahoo.ycsb.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import net.citrusleaf.CitrusleafClient;
import net.citrusleaf.CitrusleafClient.ClResult;
import net.citrusleaf.CitrusleafClient.ClResultCode;

import com.yahoo.ycsb.ByteArrayByteIterator;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DBException;

public class AerospikeClient extends com.yahoo.ycsb.DB{
	
	private CitrusleafClient cl;

	public static  String NAMESPACE = "test";

	public static  String SET = "YCSB";
	
	public static final int OK = 0;
	public static final int ERROR = -1;
	
	public void init() throws DBException {
		Properties props = getProperties();
		int port;

		//retrieve port
		String portString = props.getProperty("port");
		if (portString != null) {
			port = Integer.parseInt(portString);
		}
		else {
			port = 3000;
		}

		//retrieve host
		String host = props.getProperty("host");
		if(host == null) {
			host = "localhost";
		}
		
		//retrieve namespace
		String ns = props.getProperty("ns");
		if(ns !=  null ) {
			NAMESPACE = ns;
		}

		//retrieve set
		String st = props.getProperty("set");
		if(st != null) {
			SET = st;
		}

		cl = new CitrusleafClient(host, port);
		try {
			//Sleep so that the partition hashmap is created by the client
			Thread.sleep(2000);
		}
		catch (InterruptedException ex) {
		}
		
		if (!cl.isConnected()) {
			throw new DBException(String.format("Failed to add %s:%d to cluster.", 
				host, port));
		}

	}

		public void cleanup() throws DBException {
		cl.close();
	}
	@Override
	public int read(String table, String key, Set<String> fields,
			HashMap<String, ByteIterator> result) {

		if(fields != null) {
			for(String bin : fields) {
				ClResult res = cl.get(NAMESPACE, SET, key, bin, null);
				if (res.result != null){
					result.put(bin, new ByteArrayByteIterator(res.result.toString().getBytes()));
				}
				else {
					return ERROR;
				}
			}
			return OK;
		}
		else {
			ClResult res = cl.getAll(NAMESPACE, SET, key, null);
			if(res.resultCode == ClResultCode.OK) {
				for(Map.Entry<String, Object> entry : res.results.entrySet()) {
					result.put(entry.getKey(), new ByteArrayByteIterator(entry.getValue().toString().getBytes()));
				}
				return OK;
			}
			return ERROR;
		}
	}

	@Override
	public int scan(String table, String startkey, int recordcount,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public int update(String table, String key,
			HashMap<String, ByteIterator> values) {
		Map<String, Object> v = new HashMap<String, Object>();

		for (Map.Entry<String, ByteIterator> entry : values.entrySet()){
			v.put(entry.getKey(), entry.getValue().toString());
		}

		ClResultCode rc = cl.set(NAMESPACE, SET, key, v, null, null);
		if(rc == ClResultCode.OK) {
			return OK;
		}

		return ERROR;
	}

	@Override
	public int insert(String table, String key,
			HashMap<String, ByteIterator> values) {
		Map<String, Object> v = new HashMap<String, Object>();

                for (Map.Entry<String, ByteIterator> entry : values.entrySet()){
                        v.put(entry.getKey(), entry.getValue().toString());
                }

                ClResultCode rc = cl.set(NAMESPACE, SET, key, v, null, null);
                if(rc == ClResultCode.OK) {
                        return OK;
                }
		return ERROR;
	}

	@Override
	public int delete(String table, String key) {
		cl.delete(NAMESPACE, SET, key, null, null);
		return 0;
	}

}
