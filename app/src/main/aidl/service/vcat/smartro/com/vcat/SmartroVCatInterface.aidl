// SmartroVCatInterface.aidl
package service.vcat.smartro.com.vcat;

import service.vcat.smartro.com.vcat.SmartroVCatCallback;

interface SmartroVCatInterface {
    void executeService(in String strJSON, in SmartroVCatCallback svcbPoint);   // V-CAT 서비스 호출
    void postExtraData(in String strJSON);  // V-CAT을 통해 실행 중인 현재 세선에 추가 데이터 전달
    void cancelService();   // V-CAT이 현재 실행 중인 세션을 강제로 종료
}