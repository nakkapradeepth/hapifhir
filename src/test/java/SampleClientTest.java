import ca.uhn.fhir.rest.client.api.IGenericClient;
import junit.framework.TestCase;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

public class SampleClientTest extends TestCase {

    @ParameterizedTest
    @CsvSource(value = {
            "James,Smith,1985-03-12", // has firstName, lastName, birthDate
            "Emma,null,1990-07-25", // only firstName, birthDate
            "null,Williams,1988-11-05", // only lastName, birthDate
            "null,null,1988-03-05", // only birthDate
            "Olivia,Brown,null", // only firstName, lastName
            "Noah,null,null", // only firstName
            "null,Garcia,null", // only lastName
            "null, null, null" // no values
    }, nullValues = "null")
    public void testGetPatientsByLastName(String firstName, String lastName, String dateOfBirth) {
        // given
        IGenericClient client = mock(IGenericClient.class);
        Date birthDate = dateOfBirth == null ? null : new Date(LocalDate.parse(dateOfBirth).toEpochDay());
        Bundle bundle = generatePersonResponse(firstName, lastName, birthDate);
        SampleClient sampleClient = spy(new SampleClient());
        doReturn(bundle).when(sampleClient).getPatientBundlesByLastName(eq(client), eq(lastName), eq(false));

        // when
        List<FhirPatient> patients = sampleClient.getPatientsByLastName(client, lastName, false);

        // then
        assertEquals(1, patients.size());
        assertEquals(1, patients.get(0).getNames().size());
        assertEquals(firstName, patients.get(0).getNames().get(0).getFirstName());
        assertEquals(lastName, patients.get(0).getNames().get(0).getLastName());
        assertEquals(birthDate, patients.get(0).getBirthDate());
        verify(sampleClient).getPatientBundlesByLastName(client, lastName, false);
    }

    private Bundle generatePersonResponse(String firstName, String lastName, Date birthDate) {
        Bundle bundle = new Bundle();
        Patient patient = new Patient();
        StringType givenName = new StringType(firstName);
        HumanName name = new HumanName();
        name.setGiven(Collections.singletonList(givenName));
        name.setFamily(lastName);
        patient.setName(Collections.singletonList(name));
        patient.setBirthDate(birthDate);
        Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(patient);
        bundle.setEntry(Collections.singletonList(bundleEntryComponent));
        return bundle;
    }
}