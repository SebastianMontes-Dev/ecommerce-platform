package com.ecommerce.modulos.compartido.infrastructure.security;

import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final RepositorioUsuario repositorioUsuario;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<Usuario> usuarioOpt = repositorioUsuario.findByCorreo(email);
        Usuario usuario;
        
        if (usuarioOpt.isEmpty()) {
            usuario = new Usuario(email, "", givenName != null ? givenName : name, familyName != null ? familyName : "");
            usuario.verifyEmail();
            usuario.addRole(RolUsuario.CUSTOMER);
            repositorioUsuario.save(usuario);
        } else {
            usuario = usuarioOpt.get();
        }

        // Return a DefaultOAuth2User mapped with authorities if needed, but for simplicity returning standard mapped user
        // You could map the user roles to GrantedAuthorities here
        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "email" // Name attribute key
        );
    }
}
