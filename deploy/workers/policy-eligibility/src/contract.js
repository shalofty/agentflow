/** Checked-in contract mirrored for Worker public WSDL/XSD serving. */

export const XSD = `<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:tns="http://agentflow.com/policy-eligibility"
           targetNamespace="http://agentflow.com/policy-eligibility"
           elementFormDefault="qualified">

  <xs:simpleType name="EligibilityDecision">
    <xs:restriction base="xs:string">
      <xs:enumeration value="ELIGIBLE"/>
      <xs:enumeration value="MANUAL_REVIEW"/>
      <xs:enumeration value="INELIGIBLE"/>
    </xs:restriction>
  </xs:simpleType>

  <xs:element name="CheckEligibilityRequest">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="applicationId" type="xs:string"/>
        <xs:element name="riskScore" type="xs:int"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>

  <xs:element name="CheckEligibilityResponse">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="applicationId" type="xs:string"/>
        <xs:element name="decision" type="tns:EligibilityDecision"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>
`;

export function WSDL(soapAddress) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<wsdl:definitions
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:tns="http://agentflow.com/policy-eligibility"
    targetNamespace="http://agentflow.com/policy-eligibility">
  <wsdl:types>
    <xs:schema targetNamespace="http://agentflow.com/policy-eligibility"
               elementFormDefault="qualified">
      <xs:simpleType name="EligibilityDecision">
        <xs:restriction base="xs:string">
          <xs:enumeration value="ELIGIBLE"/>
          <xs:enumeration value="MANUAL_REVIEW"/>
          <xs:enumeration value="INELIGIBLE"/>
        </xs:restriction>
      </xs:simpleType>
      <xs:element name="CheckEligibilityRequest">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="applicationId" type="xs:string"/>
            <xs:element name="riskScore" type="xs:int"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
      <xs:element name="CheckEligibilityResponse">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="applicationId" type="xs:string"/>
            <xs:element name="decision" type="tns:EligibilityDecision"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>
  </wsdl:types>
  <wsdl:message name="CheckEligibilityRequest">
    <wsdl:part name="parameters" element="tns:CheckEligibilityRequest"/>
  </wsdl:message>
  <wsdl:message name="CheckEligibilityResponse">
    <wsdl:part name="parameters" element="tns:CheckEligibilityResponse"/>
  </wsdl:message>
  <wsdl:portType name="PolicyEligibilityPort">
    <wsdl:operation name="CheckEligibility">
      <wsdl:input message="tns:CheckEligibilityRequest"/>
      <wsdl:output message="tns:CheckEligibilityResponse"/>
    </wsdl:operation>
  </wsdl:portType>
  <wsdl:binding name="PolicyEligibilityPortSoap11" type="tns:PolicyEligibilityPort">
    <soap:binding transport="http://schemas.xmlsoap.org/soap/http" style="document"/>
    <wsdl:operation name="CheckEligibility">
      <soap:operation soapAction=""/>
      <wsdl:input><soap:body use="literal"/></wsdl:input>
      <wsdl:output><soap:body use="literal"/></wsdl:output>
    </wsdl:operation>
  </wsdl:binding>
  <wsdl:service name="PolicyEligibilityPortService">
    <wsdl:port name="PolicyEligibilityPortSoap11" binding="tns:PolicyEligibilityPortSoap11">
      <soap:address location="${soapAddress}"/>
    </wsdl:port>
  </wsdl:service>
</wsdl:definitions>
`;
}
