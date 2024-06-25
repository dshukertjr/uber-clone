insert into auth.users (instance_id, id, aud, role, encrypted_password, raw_app_meta_data, raw_user_meta_data, email_confirmed_at, created_at, is_anonymous)
  values ('b9f2f5eb-904b-4c50-95ac-b1654e0e2d3c', '185f2f83-d63a-4c9b-b4a0-7e4a885799e2', 'authenticated', 'authenticated', '$2a$10$6gPtvpqCAiwavx1EOnjIgOykKMgzRdiBuejUQGIRRjvUi/ZgMh.9C', '{}', '{}', timezone('utc'::text, now()), timezone('utc'::text, now()), true);


insert into public.drivers (id, model, number, location, is_available) 
    values 
        ('3ac03221-ad55-4eea-aa3b-3828335d0d5a', 'Toyota Corolla', 'ABC-123', ST_GeographyFromText('SRID=4326;POINT(-73.9940147 40.7531084)'), false)
    , ('51d89275-8825-4a82-bba1-f246f595f5e9', 'Honda Civic', 'DEF-456', ST_GeographyFromText('SRID=4326;POINT(-73.9950147 40.7532074)'), true)
    , ('a040ae05-3928-4cbf-8577-bbad6125c3fe', 'Ford Focus', 'GHI-789', ST_GeographyFromText('SRID=4326;POINT(-73.9939147 40.7530074)'), true);

insert into public.rides (status, driver_id, passenger_id, origin, destination, fare)
    values
        ('picking_up', '3ac03221-ad55-4eea-aa3b-3828335d0d5a', '185f2f83-d63a-4c9b-b4a0-7e4a885799e2', ST_GeographyFromText('SRID=4326;POINT(-122.3321 47.6062)'), ST_GeographyFromText('SRID=4326;POINT(-122.3521 47.5162)'), 1000),
        ('completed', '3ac03221-ad55-4eea-aa3b-3828335d0d5a', '185f2f83-d63a-4c9b-b4a0-7e4a885799e2', ST_GeographyFromText('SRID=4326;POINT(-122.3321 47.6062)'), ST_GeographyFromText('SRID=4326;POINT(-122.3521 47.5162)'), 1000);