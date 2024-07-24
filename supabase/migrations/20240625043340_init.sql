-- Example: enable the "postgis" extension
create extension postgis with schema extensions;

create table if not exists public.drivers (
	id uuid primary key default gen_random_uuid(),
	model text not null,
    number text not null,
    is_available boolean not null default false,
	location geography(POINT) not null,
    latitude double precision generated always as (st_y(location::geometry)) stored,
    longitude double precision generated always as (st_x(location::geometry)) stored
);
comment on table public.drivers is 'Holds the list of drivers and their locations';

alter table public.drivers enable row level security;
create policy "Any authenticated users can select drivers." on public.drivers for select to authenticated using (true);
create policy "Drivers can update their own status." on public.drivers for update to authenticated using (auth.uid() = id);

alter publication supabase_realtime add table public.drivers;

create type ride_status as enum ('picking_up', 'riding', 'completed');

create table if not exists public.rides (
    id uuid primary key default gen_random_uuid(),
    driver_id uuid not null references public.drivers(id),
    passenger_id uuid not null references auth.users(id),
    origin geography(POINT) not null,
    destination geography(POINT) not null,
    fare integer not null,
    status ride_status not null default 'picking_up'
);
comment on table public.rides is 'A new ride is created whenever a user requests a ride.';

alter table public.rides enable row level security;
create policy "The driver or the passenger can select the ride." on public.rides for select to authenticated using (driver_id = auth.uid() or passenger_id = auth.uid());
create policy "The driver can update the status. " on public.rides for update to authenticated using (auth.uid() = driver_id);

alter publication supabase_realtime add table public.rides;

-- Create a trigger to update the driver status
create function update_driver_status()
    returns trigger
    language plpgsql
    as $$
        begin
            if new.status = 'completed' then
                update public.drivers
                set is_available = true
                where id = new.driver_id;
            else
                update public.drivers
                set is_available = false
                where id = new.driver_id;
            end if;
            return new;
    end $$;

create trigger driver_status_update_trigger
after insert or update on rides
for each row
execute function update_driver_status();

-- Finds the closest available driver within 3000m radius
create function public.find_driver(origin geography(POINT), destination geography(POINT), fare int)
    returns table(driver_id uuid, ride_id uuid)
    language plpgsql
    as $$
        declare
            v_driver_id uuid;
            v_ride_id uuid;
        begin
            select 
                drivers.id into v_driver_id
            from public.drivers
            where is_available = true
                and st_dwithin(origin, location, 3000) 
            order by drivers.location <-> origin
            limit 1;

            -- return null if no available driver is found
            if v_driver_id is null then
                return;
            end if;

            insert into public.rides (driver_id, passenger_id, origin, destination, fare)
            values (v_driver_id, auth.uid(), origin, destination, fare)
            returning id into v_ride_id;

            return query
                select v_driver_id as driver_id, v_ride_id as ride_id;
    end $$ security definer;


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
