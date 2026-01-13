## Scenario config descriptions

This directory contains various load generation scenario configs that can be used
to simulate different types of workloads.

- **default-scenarios**: These scenarios provide a very easy scenario for general testing.
  <br><br>
- **low-scenarios**: These scenarios generate a high amount of easy queries.<br>
  Expected behaviour: no problem for OpenSearch, no errors
  <br><br>
- **medium-scenarios**: These scenarios generate a balanced mix of easy and complex queries.<br>
  Expected behaviour: OpenSearch should handle the load with minimal errors
  <br><br>
- **high-scenarios**: These scenarios generate a low amount of complex queries.<br>
  Expected behaviour: OpenSearch may have problems, errors are expected
  <br><br>
- **long-scenarios**: These scenarios generate a balanced mix of easy and complex queries over an extended period.<br>
  Expected behaviour: OpenSearch should handle the load with minimal errors over time
