package com.example.bee.entities.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "chat_lieu",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_lieu_ma", columnNames = "ma")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatLieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma", length = 50, nullable = false, unique = true)
    private String ma;

    @Column(name = "ten", length = 150, nullable = false)
    private String ten;

    @Column(name = "mo_ta", length = 255)
    private String moTa;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @OneToMany(mappedBy = "chatLieu")
    @JsonIgnore // <--- THÊM CÁI NÀY VÀO! QUAN TRỌNG VÃI LỒN!
    private List<SanPham> sanPhams;
}
