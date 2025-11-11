package ru.otus.crm.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
public class Client implements Cloneable {

    @Id
    @SequenceGenerator(name = "client_gen", sequenceName = "client_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_gen")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER) // Изменил на EAGER для тестирования
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true) // Изменил на EAGER для тестирования
    private List<Phone> phones = new ArrayList<>();

    public Client(String name) {
        this.id = null;
        this.name = name;
    }

    public Client(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Client(Long id, String name, Address address, List<Phone> phones) {
        this.id = id;
        this.name = name;
        this.address = address;
        if (phones != null) {
            this.phones = new ArrayList<>(phones);
            this.phones.forEach(phone -> phone.setClient(this));
        }
    }

    public void setPhones(List<Phone> phones) {
        if (this.phones != null) {
            this.phones.clear();
            if (phones != null) {
                this.phones.addAll(phones);
                this.phones.forEach(phone -> phone.setClient(this));
            }
        } else {
            this.phones = phones;
            if (phones != null) {
                phones.forEach(phone -> phone.setClient(this));
            }
        }
    }

    public void addPhone(Phone phone) {
        if (phones == null) {
            phones = new ArrayList<>();
        }
        phones.add(phone);
        phone.setClient(this);
    }

    @Override
    @SuppressWarnings({"java:S2975", "java:S1182"})
    public Client clone() {
        Address clonedAddress = null;
        if (this.address != null) {
            clonedAddress = new Address(this.address.getId(), this.address.getStreet());
        }

        List<Phone> clonedPhones = null;
        if (this.phones != null) {
            clonedPhones = new ArrayList<>();
            for (Phone phone : this.phones) {
                Phone clonedPhone = new Phone(phone.getId(), phone.getNumber());
                clonedPhones.add(clonedPhone);
            }
        }

        return new Client(this.id, this.name, clonedAddress, clonedPhones);
    }

    @Override
    public String toString() {
        return "Client{" + "id=" + id + ", name='" + name + '\'' + ", address=" + address + ", phones=" + phones + '}';
    }
}
