package model;

import java.util.List;
import java.util.Objects;

public class PhoneNumber {
    public PhoneNumber() {
    }

    private List<Integer> phoneNumbers;

    public List<Integer> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<Integer> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
        return Objects.equals(phoneNumbers, that.phoneNumbers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumbers);
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                "phoneNumbers=" + phoneNumbers +
                '}';
    }
}
