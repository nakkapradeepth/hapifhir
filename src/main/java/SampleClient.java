import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SampleClient {

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Expecting patient last names file location");
            return;
        }

        String fileLocation = args[0];
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        client.registerInterceptor(new LoggingInterceptor(false));

        final SampleClient sampleClient = new SampleClient();
        sampleClient.calculateAvgRespTimesForPatientSearch(client, fileLocation);
    }

    /**
     * Perform multiple patient resource search operations based on last name that are read from fileLocation
     * @param client http client
     * @param fileLocation location of file contains patient last names
     * @throws IOException exception thrown when there is an issue reading content from given file
     */
    private void calculateAvgRespTimesForPatientSearch(IGenericClient client, String fileLocation) throws IOException {
        ResponseTimeInterceptor responseTimeInterceptor = new ResponseTimeInterceptor();
        client.registerInterceptor(responseTimeInterceptor);
        String name;
        for (int i = 0; i < 3; i++) {
            try (BufferedReader br = new BufferedReader(new FileReader(fileLocation))) {
                while ((name = br.readLine()) != null) {
                    boolean lastRun = i==2;
                    getPatientsByLastName(client, name, lastRun).forEach(System.out::println);
                }
            }
            System.out.println("Avg resp time: " + responseTimeInterceptor.getAvgRespTime());
            // clear response times to calculate avg for next run
            responseTimeInterceptor.clear();
        }
    }

    /**
     * Makes HTTP call and returns the deserialized http response in the form of {@link Bundle}
     * @param client http client
     * @param lastName patient last name
     * @param noCache flag to not use cache
     * @return bundle
     */
    public Bundle getPatientBundlesByLastName(IGenericClient client, String lastName, boolean noCache) {
        return client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value(lastName))
                .cacheControl(noCache ? new CacheControlDirective().setNoCache(true) : null)
                .returnBundle(Bundle.class)
                .execute();
    }

    /**
     * Fetches patients with given last name and generates a list of {@link FhirPatient}
     * @param client http client
     * @param lastName patient last name
     * @param noCache flag to not use cache
     * @return list of {@link FhirPatient}
     */
    public List<FhirPatient> getPatientsByLastName(IGenericClient client, String lastName, boolean noCache) {
        final Bundle response = getPatientBundlesByLastName(client, lastName, noCache);
        final List<FhirPatient> patients = new ArrayList<>(response.getEntry().size());
        for (Bundle.BundleEntryComponent entry : response.getEntry()) {
            final FhirPatient fhirPatient = new FhirPatient();
            final Resource resource = entry.getResource();

            if (!ResourceType.Patient.equals(resource.getResourceType())) {
                continue;
            }

            final Patient patient = (Patient) resource;
            final List<FhirPatient.Name> names = new ArrayList<>(patient.getName().size());
            for (HumanName name : patient.getName()) {
                FhirPatient.Name patientName = new FhirPatient.Name();
                final String givenName = getFullName(name.getGiven());
                if (!givenName.isEmpty()) {
                    patientName.setFirstName(givenName);
                }
                patientName.setLastName(name.getFamily());
                names.add(patientName);
            }
            fhirPatient.setNames(names);
            fhirPatient.setBirthDate(patient.getBirthDate());
            patients.add(fhirPatient);
        }
        return patients;
    }

    private String getFullName(List<StringType> names) {
        return names.stream()
                .map(StringType::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

}
