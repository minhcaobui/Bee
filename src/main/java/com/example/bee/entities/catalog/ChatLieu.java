package com.example.bee.entities.catalog;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_lieu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatLieu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(max = 20, message = "Mã tối đa 20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Mã không được chứa tiếng Việt hoặc ký tự đặc biệt")
    private String ma;

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String ten;

    @Column(name = "mo_ta", columnDefinition = "LONGTEXT")
    private String moTa;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "ngay_sua")
    private LocalDateTime ngaySua;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}