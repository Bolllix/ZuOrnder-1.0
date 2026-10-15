package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * Person – ein Teilnehmer
 * ============================================================================
 * Wichtig für die Regeln:
 *   - gender    : "maennlich", "weiblich" oder "divers" (für die Geschlechtertrennung)
 *   - partnerId : Paare bekommen dieselbe Paar-ID (z.B. "P1") -> "Paare im selben Zimmer"
 *   - groupId   : Gruppe/Klasse (z.B. "Klasse 8a")           -> "Gruppenzusammenhalt"
 *   - age       : z.B. für "Senioren nicht ins obere Hochbett"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Person {
    private String id;
    private String firstname;
    private String lastname;
    /** "maennlich", "weiblich", "divers" */
    private String gender;
    private int age;
    /** Paar-Kennung (z.B. "P1"): beide Partner haben dieselbe */
    private String partnerId;
    /** Gruppen-Kennung (z.B. "G1" oder "Klasse 8a") */
    private String groupId;
    /** Wunsch-Etage (null = egal); Integer statt int, damit "kein Wunsch" möglich ist */
    private Integer desiredFloor;
    private String desiredRoom;
    /** Besondere Anforderungen, z.B. "Rollstuhl" oder "Asthma" */
    private String specialNeeds;
    /** Zusätzliche Spalten aus dem Import (Spaltenname -> Wert) */
    private Map<String, Object> customAttributes;

    public Person() {
        this.id = UUID.randomUUID().toString();
        this.customAttributes = new HashMap<>();
    }

    public Person(String lastname, String firstname, String gender, int age, String partnerId) {
        this();
        this.lastname = lastname;
        this.firstname = firstname;
        this.gender = gender;
        this.age = age;
        this.partnerId = partnerId;
    }

    public Person(String lastname, String firstname, String gender, int age, String partnerId, String groupId) {
        this(lastname, firstname, gender, age, partnerId);
        this.groupId = groupId;
    }

    public Person(String id, String lastname, String firstname, String gender, int age, String partnerId, String groupId) {
        this(lastname, firstname, gender, age, partnerId);
        if (id != null && !id.trim().isEmpty()) {
            this.id = id;
        }
        this.groupId = groupId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Integer getDesiredFloor() {
        return desiredFloor;
    }

    public void setDesiredFloor(Integer desiredFloor) {
        this.desiredFloor = desiredFloor;
    }

    public String getDesiredRoom() {
        return desiredRoom;
    }

    public void setDesiredRoom(String desiredRoom) {
        this.desiredRoom = desiredRoom;
    }

    public String getSpecialNeeds() {
        return specialNeeds;
    }

    public void setSpecialNeeds(String specialNeeds) {
        this.specialNeeds = specialNeeds;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    /** "Vorname Nachname" (fehlende Teile werden weggelassen) */
    public String getFullName() {
        return ((firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "")).trim();
    }
}
