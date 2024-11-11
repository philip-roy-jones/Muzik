# Authentication and Authorization Setup TODOs

- [ ] **Backend**
    - [ ] Add user roles to token payload
    - [x] Add roles to user object in the database
    - [ ] Configure authorization roles in SecurityConfig

- [ ] **Frontend Integration**
    - [ ] Upon page load, check if the user is authenticated and authorized through backend request that will
      automatically have access and refresh token in httpOnly cookies
    - [ ] Before every request the user makes, check if the user is authenticated and has the required role
    - [ ] Display the appropriate UI based on the user's role