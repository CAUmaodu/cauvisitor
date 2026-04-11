-- poi.csv 中名称较长，若建库时 cau_point.name 为 varchar(20) 会导致写入截断或检索异常。
-- 在 PostgreSQL 中执行一次即可：

ALTER TABLE public.cau_point
    ALTER COLUMN name TYPE varchar(255);
