// IDataService.aidl
package kr.durian.duriancam.service;

// Declare any non-default types here with import statements

import kr.durian.duriancam.service.IDataServiceCallback;

interface IDataService {

    boolean registerCallback(IDataServiceCallback callback);
    boolean unregisterCallback(IDataServiceCallback callback);
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    void connectWebSocket();
}
