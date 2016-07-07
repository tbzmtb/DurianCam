// IDataServiceCallback.aidl
package kr.durian.duriancam.service;

interface IDataServiceCallback {
	oneway void valueChanged(long value);
}
