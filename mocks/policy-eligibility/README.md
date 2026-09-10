# Policy Eligibility SOAP Mock

This Spring Boot/Spring-WS mock keeps its contract and behavior easy to inspect: the source contract is [`src/main/resources/xsd/policy-eligibility.xsd`](src/main/resources/xsd/policy-eligibility.xsd), a checked-in WSDL is at [`src/main/resources/wsdl/policy-eligibility.wsdl`](src/main/resources/wsdl/policy-eligibility.wsdl), the generated runtime WSDL is published at `http://localhost:8092/ws/policyEligibility.wsdl`, the SOAP operation is implemented by [`src/main/java/com/agentflow/mocks/eligibility/PolicyEligibilityEndpoint.java`](src/main/java/com/agentflow/mocks/eligibility/PolicyEligibilityEndpoint.java), and SOAP fault handling is visible in [`src/main/java/com/agentflow/mocks/eligibility/PolicyEligibilitySoapFault.java`](src/main/java/com/agentflow/mocks/eligibility/PolicyEligibilitySoapFault.java). Risk scores below 50 return `ELIGIBLE`, 50–79 return `MANUAL_REVIEW`, and 80 or above return `INELIGIBLE`.

## Sample request

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pol="http://agentflow.com/policy-eligibility">
  <soapenv:Header/>
  <soapenv:Body>
    <pol:CheckEligibilityRequest>
      <pol:applicationId>application-123</pol:applicationId>
      <pol:riskScore>27</pol:riskScore>
    </pol:CheckEligibilityRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Save the envelope as `request.xml`, then run:

```bash
curl -sS -H 'Content-Type: text/xml' \
  --data-binary @request.xml http://localhost:8092/ws
```

Set an admin mode with `POST /admin/faults`: `none`, `manual_review`, or `fault`.

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"mode":"fault"}' http://localhost:8092/admin/faults
```
