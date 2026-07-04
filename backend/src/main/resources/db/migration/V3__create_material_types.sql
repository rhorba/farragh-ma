CREATE TABLE material_types (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code        VARCHAR(50) NOT NULL UNIQUE,
  label_fr    VARCHAR(100) NOT NULL,
  label_ar    VARCHAR(100) NOT NULL
);

INSERT INTO material_types (code, label_fr, label_ar) VALUES
  ('PLASTIC',    'Plastique',     'بلاستيك'),
  ('METAL',      'Métal',         'معدن'),
  ('ELECTRONIC', 'Électronique',  'إلكترونيات'),
  ('ORGANIC',    'Organique',     'عضوي'),
  ('PAPER',      'Papier',        'ورق'),
  ('GLASS',      'Verre',         'زجاج');
