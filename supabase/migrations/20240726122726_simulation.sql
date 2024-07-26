-- A function that is only used in the simulation script
create function public.get_ride_and_driver(ride_id uuid)
    returns table(driver_id uuid, origin json, destination json, driver_location json)
    language sql
    as $$
    select 
        rides.driver_id,
        json_build_object(
            'lat', st_y(rides.origin::geometry),
            'lng', st_x(rides.origin::geometry)
        ) as origin,
        json_build_object(
            'lat', st_y(rides.destination::geometry),
            'lng', st_x(rides.destination::geometry)
        ) as destination,
        json_build_object(
            'lat', st_y(drivers.location::geometry),
            'lng', st_x(drivers.location::geometry)
        ) as driver_location
    from public.rides
    join public.drivers on drivers.id = rides.driver_id
    where rides.id = ride_id;
 $$ security invoker;
