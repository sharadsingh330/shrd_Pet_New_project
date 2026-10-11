-- Roles
create type public.app_role as enum ('admin','shelter','adopter');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  email text not null default '',
  phone text not null default '',
  city text not null default '',
  bio text not null default '',
  created_at timestamptz not null default now()
);
grant select, insert, update on public.profiles to authenticated;
grant all on public.profiles to service_role;
alter table public.profiles enable row level security;

create table public.user_roles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.app_role not null,
  unique (user_id, role)
);
grant select on public.user_roles to authenticated;
grant all on public.user_roles to service_role;
alter table public.user_roles enable row level security;

create or replace function public.has_role(_user_id uuid, _role public.app_role)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.user_roles where user_id = _user_id and role = _role)
$$;

create table public.shelters (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid references auth.users(id) on delete set null,
  name text not null,
  city text not null default '',
  email text not null default '',
  verified boolean not null default false,
  created_at timestamptz not null default now()
);
grant select on public.shelters to anon;
grant select, insert, update on public.shelters to authenticated;
grant all on public.shelters to service_role;
alter table public.shelters enable row level security;

create table public.pets (
  id uuid primary key default gen_random_uuid(),
  shelter_id uuid not null references public.shelters(id) on delete cascade,
  name text not null,
  species text not null default 'Dog',
  breed text not null default '',
  age_label text not null default '',
  size text not null default 'Medium',
  temperament text not null default '',
  city text not null default '',
  description text not null default '',
  image_url text,
  image_key text not null default 'dog',
  match_score int not null default 85,
  adoption_status text not null default 'available',
  listing_status text not null default 'pending',
  created_at timestamptz not null default now()
);
grant select on public.pets to anon;
grant select, insert, update, delete on public.pets to authenticated;
grant all on public.pets to service_role;
alter table public.pets enable row level security;

create table public.applications (
  id uuid primary key default gen_random_uuid(),
  pet_id uuid not null references public.pets(id) on delete cascade,
  adopter_id uuid not null references auth.users(id) on delete cascade,
  adopter_name text not null default '',
  adopter_email text not null default '',
  housing text not null default '',
  experience text not null default '',
  note text not null default '',
  status text not null default 'pending',
  created_at timestamptz not null default now()
);
grant select, insert, update on public.applications to authenticated;
grant all on public.applications to service_role;
alter table public.applications enable row level security;

create table public.messages (
  id uuid primary key default gen_random_uuid(),
  application_id uuid not null references public.applications(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  sender_name text not null default '',
  body text not null,
  created_at timestamptz not null default now()
);
grant select, insert on public.messages to authenticated;
grant all on public.messages to service_role;
alter table public.messages enable row level security;

create table public.system_settings (
  key text primary key,
  value text not null default '',
  updated_at timestamptz not null default now()
);
grant select on public.system_settings to anon;
grant select on public.system_settings to authenticated;
grant all on public.system_settings to service_role;
alter table public.system_settings enable row level security;

-- helper: does user own the shelter that owns this pet
create or replace function public.owns_pet(_user_id uuid, _pet_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.pets p
    join public.shelters s on s.id = p.shelter_id
    where p.id = _pet_id and s.owner_id = _user_id
  )
$$;

-- Policies
create policy "own profile read" on public.profiles for select to authenticated using (auth.uid() = id or public.has_role(auth.uid(),'admin'));
create policy "own profile insert" on public.profiles for insert to authenticated with check (auth.uid() = id);
create policy "own profile update" on public.profiles for update to authenticated using (auth.uid() = id or public.has_role(auth.uid(),'admin'));

create policy "own roles read" on public.user_roles for select to authenticated using (auth.uid() = user_id or public.has_role(auth.uid(),'admin'));

create policy "shelters public read" on public.shelters for select to anon using (true);
create policy "shelters auth read" on public.shelters for select to authenticated using (true);
create policy "shelters insert own" on public.shelters for insert to authenticated with check (auth.uid() = owner_id);
create policy "shelters update own" on public.shelters for update to authenticated using (auth.uid() = owner_id or public.has_role(auth.uid(),'admin'));

create policy "pets public read" on public.pets for select to anon using (listing_status = 'approved');
create policy "pets auth read" on public.pets for select to authenticated using (
  listing_status = 'approved'
  or public.has_role(auth.uid(),'admin')
  or exists (select 1 from public.shelters s where s.id = shelter_id and s.owner_id = auth.uid())
);
create policy "pets insert by shelter" on public.pets for insert to authenticated with check (
  exists (select 1 from public.shelters s where s.id = shelter_id and s.owner_id = auth.uid())
);
create policy "pets update" on public.pets for update to authenticated using (
  public.has_role(auth.uid(),'admin')
  or exists (select 1 from public.shelters s where s.id = shelter_id and s.owner_id = auth.uid())
);
create policy "pets delete by shelter" on public.pets for delete to authenticated using (
  exists (select 1 from public.shelters s where s.id = shelter_id and s.owner_id = auth.uid())
);

create policy "applications read" on public.applications for select to authenticated using (
  auth.uid() = adopter_id or public.has_role(auth.uid(),'admin') or public.owns_pet(auth.uid(), pet_id)
);
create policy "applications insert own" on public.applications for insert to authenticated with check (auth.uid() = adopter_id);
create policy "applications update" on public.applications for update to authenticated using (
  public.has_role(auth.uid(),'admin') or public.owns_pet(auth.uid(), pet_id)
);

create policy "messages read" on public.messages for select to authenticated using (
  exists (
    select 1 from public.applications a where a.id = application_id
    and (a.adopter_id = auth.uid() or public.owns_pet(auth.uid(), a.pet_id) or public.has_role(auth.uid(),'admin'))
  )
);
create policy "messages insert" on public.messages for insert to authenticated with check (
  sender_id = auth.uid() and exists (
    select 1 from public.applications a where a.id = application_id
    and (a.adopter_id = auth.uid() or public.owns_pet(auth.uid(), a.pet_id) or public.has_role(auth.uid(),'admin'))
  )
);

create policy "settings read" on public.system_settings for select to anon using (true);
create policy "settings read auth" on public.system_settings for select to authenticated using (true);
create policy "settings write admin" on public.system_settings for update to authenticated using (public.has_role(auth.uid(),'admin'));

-- New user bootstrap
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  _role public.app_role;
  _shelter_name text;
begin
  _role := coalesce(nullif(new.raw_user_meta_data->>'role','')::public.app_role, 'adopter');
  insert into public.profiles (id, full_name, email)
  values (new.id, coalesce(new.raw_user_meta_data->>'full_name',''), coalesce(new.email,''));
  insert into public.user_roles (user_id, role) values (new.id, _role) on conflict do nothing;
  if _role = 'shelter' then
    _shelter_name := coalesce(nullif(new.raw_user_meta_data->>'shelter_name',''), coalesce(new.raw_user_meta_data->>'full_name','New Shelter'));
    insert into public.shelters (owner_id, name, city, email, verified)
    values (new.id, _shelter_name, coalesce(new.raw_user_meta_data->>'city',''), coalesce(new.email,''), false);
  end if;
  return new;
end;
$$;

create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

-- Seed data
insert into public.system_settings (key, value) values
  ('platform_name','Pawlift'),
  ('applications_open','true'),
  ('auto_approve_listings','false'),
  ('support_email','hello@pawlift.app');

insert into public.shelters (id, name, city, email, verified) values
  ('11111111-1111-1111-1111-111111111111','Riverside Rescue','Portland, OR','team@riversiderescue.org', true),
  ('22222222-2222-2222-2222-222222222222','Wingfield Haven','Eugene, OR','hello@wingfieldhaven.org', false);

insert into public.pets (shelter_id, name, species, breed, age_label, size, temperament, city, description, image_key, match_score, adoption_status, listing_status) values
  ('11111111-1111-1111-1111-111111111111','Juniper','Cat','Domestic Shorthair','3 yr','Small','Playful','Portland, OR','Juniper is a curious window-watcher who purrs the moment you sit down. Litter trained, vaccinated and great with older children.','cat',91,'available','approved'),
  ('11111111-1111-1111-1111-111111111111','Biscuit','Dog','Beagle','8 mo','Medium','High energy','Portland, OR','Biscuit needs a garden and a running buddy. Crate trained, microchipped, and endlessly happy.','beagle',78,'available','approved'),
  ('11111111-1111-1111-1111-111111111111','Winston','Dog','Greyhound','6 yr','Large','Calm','Portland, OR','Winston is a retired racer who now sleeps 18 hours a day. Perfect for a quiet apartment home.','greyhound',85,'available','approved'),
  ('11111111-1111-1111-1111-111111111111','Maple','Dog','Golden Retriever','2 yr','Large','Gentle','Portland, OR','Maple greets everyone like an old friend. Fully vetted, good with cats, kids and chaos.','golden',94,'available','approved'),
  ('22222222-2222-2222-2222-222222222222','Pepper','Bird','Cockatiel','1 yr','Small','Chatty','Eugene, OR','Pepper whistles three tunes and is hand tame. Comes with cage and food.','dog',72,'available','pending'),
  ('22222222-2222-2222-2222-222222222222','Clover','Cat','Tabby','5 yr','Small','Shy','Eugene, OR','Clover is slow to trust and worth every minute of it. Best as the only pet in a calm home.','cat',80,'available','pending');
